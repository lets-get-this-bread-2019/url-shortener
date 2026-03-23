package com.example.urlshortener.controller;

import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
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
    public RedirectView redirectToOriginal(@PathVariable String code) {
        return urlService.findByCode(code)
            .map(shortUrl -> {
                if (shortUrl.isExpired()) {
                    throw new UrlExpiredException("Short URL has expired: " + code);
                }
                RedirectView redirectView = new RedirectView(shortUrl.originalUrl());
                redirectView.setStatusCode(HttpStatus.FOUND); // 302
                return redirectView;
            })
            .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + code));
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
