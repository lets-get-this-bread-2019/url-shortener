package com.example.urlshortener.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("short_urls")
public record ShortUrl(
    @Id Long id,
    String code,
    String originalUrl,
    Instant expiresAt
) {
    public ShortUrl(String code, String originalUrl) {
        this(null, code, originalUrl, null);
    }

    public ShortUrl(String code, String originalUrl, Instant expiresAt) {
        this(null, code, originalUrl, expiresAt);
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
