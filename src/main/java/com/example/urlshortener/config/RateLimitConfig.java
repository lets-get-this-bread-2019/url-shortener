package com.example.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    private int shortenRequestsPerMinute = 10;
    private int redirectRequestsPerMinute = 100;

    public int getShortenRequestsPerMinute() {
        return shortenRequestsPerMinute;
    }

    public void setShortenRequestsPerMinute(int shortenRequestsPerMinute) {
        this.shortenRequestsPerMinute = shortenRequestsPerMinute;
    }

    public int getRedirectRequestsPerMinute() {
        return redirectRequestsPerMinute;
    }

    public void setRedirectRequestsPerMinute(int redirectRequestsPerMinute) {
        this.redirectRequestsPerMinute = redirectRequestsPerMinute;
    }
}
