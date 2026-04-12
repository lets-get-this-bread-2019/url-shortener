package com.example.urlshortener.controller;

import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.service.AnalyticsService;
import com.example.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UrlController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;

    public UrlController(UrlService urlService, AnalyticsService analyticsService) {
        this.urlService = urlService;
        this.analyticsService = analyticsService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<Map<String, String>> shortenUrl(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        String url = (String) request.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }

        String customCode = (String) request.get("customCode");

        // Parse ttlSeconds if provided
        Long ttlSeconds = null;
        if (request.containsKey("ttlSeconds")) {
            Object ttlValue = request.get("ttlSeconds");
            if (ttlValue instanceof Number) {
                ttlSeconds = ((Number) ttlValue).longValue();
                if (ttlSeconds <= 0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "ttlSeconds must be positive"));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "ttlSeconds must be a number"));
            }
        }

        try {
            ShortUrl shortUrl = urlService.createShortUrl(url, customCode, ttlSeconds);

            // Build base URL from the incoming request so it works on any host
            String scheme = httpRequest.getHeader("X-Forwarded-Proto") != null
                    ? httpRequest.getHeader("X-Forwarded-Proto")
                    : httpRequest.getScheme();
            String host = httpRequest.getHeader("X-Forwarded-Host") != null
                    ? httpRequest.getHeader("X-Forwarded-Host")
                    : httpRequest.getServerName();
            int port = httpRequest.getServerPort();
            String portSuffix = (port == 80 || port == 443 || port == 0) ? "" : ":" + port;
            String shortUrlString = scheme + "://" + host + portSuffix + "/" + shortUrl.code();

            return ResponseEntity.ok(Map.of("shortUrl", shortUrlString, "code", shortUrl.code()));
        } catch (UrlService.InvalidUrlException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (UrlService.InvalidCodeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (UrlService.CodeAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{code}")
    public RedirectView redirectToOriginal(
            @PathVariable String code,
            HttpServletRequest request) {
        return urlService.findByCode(code)
            .map(shortUrl -> {
                if (shortUrl.isExpired()) {
                    throw new UrlExpiredException("Short URL has expired: " + code);
                }

                // Track click analytics
                String ipAddress = getClientIpAddress(request);
                String userAgent = request.getHeader("User-Agent");
                String referrer = request.getHeader("Referer");
                analyticsService.recordClick(shortUrl.id(), ipAddress, userAgent, referrer);

                RedirectView redirectView = new RedirectView(shortUrl.originalUrl());
                redirectView.setStatusCode(HttpStatus.FOUND); // 302
                return redirectView;
            })
            .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + code));
    }

    @GetMapping("/analytics/{code}")
    public ResponseEntity<?> getAnalytics(@PathVariable String code) {
        return analyticsService.getAnalytics(code)
            .map(analytics -> {
                Map<String, Object> response = Map.of(
                    "code", analytics.code(),
                    "originalUrl", analytics.originalUrl(),
                    "totalClicks", analytics.totalClicks(),
                    "uniqueIps", analytics.uniqueIps(),
                    "clicksByDate", analytics.clicksByDate().entrySet().stream()
                        .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            Map.Entry::getValue
                        )),
                    "topReferrers", analytics.topReferrers()
                );
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Extracts the client's IP address, handling proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP in the chain (the original client)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class UrlNotFoundException extends RuntimeException {
        public UrlNotFoundException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class UrlExpiredException extends RuntimeException {
        public UrlExpiredException(String message) {
            super(message);
        }
    }
}
