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
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        String url = request.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }

        String customCode = request.get("customCode");

        try {
            ShortUrl shortUrl = urlService.createShortUrl(url, customCode);

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
}
