package com.example.urlshortener.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("click_analytics")
public record ClickAnalytics(
    @Id Long id,
    @Column("short_url_id") Long shortUrlId,
    @Column("clicked_at") Instant clickedAt,
    @Column("ip_hash") String ipHash,
    @Column("user_agent") String userAgent,
    @Column("referrer") String referrer
) {
    public ClickAnalytics(Long shortUrlId, String ipHash, String userAgent, String referrer) {
        this(null, shortUrlId, Instant.now(), ipHash, userAgent, referrer);
    }
}
