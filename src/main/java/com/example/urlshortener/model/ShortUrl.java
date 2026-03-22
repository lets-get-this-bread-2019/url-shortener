package com.example.urlshortener.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("short_urls")
public record ShortUrl(
    @Id Long id,
    String code,
    String originalUrl
) {
    public ShortUrl(String code, String originalUrl) {
        this(null, code, originalUrl);
    }
}
