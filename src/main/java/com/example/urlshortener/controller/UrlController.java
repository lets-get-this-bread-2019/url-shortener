package com.example.urlshortener.controller;

import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.service.UrlService;
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
    public ResponseEntity<Map<String, String>> shortenUrl(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ShortUrl shortUrl = urlService.createShortUrl(url);
        String shortUrlString = "http://localhost:8080/" + shortUrl.code();

        return ResponseEntity.ok(Map.of("shortUrl", shortUrlString));
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
