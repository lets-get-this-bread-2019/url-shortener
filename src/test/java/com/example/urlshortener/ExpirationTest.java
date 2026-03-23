package com.example.urlshortener;

import com.example.urlshortener.model.ShortUrl;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ExpirationTest {

    @Test
    void testUrlWithNullExpiresAtNeverExpires() {
        ShortUrl url = new ShortUrl(1L, "abc1234", "https://example.com", null);
        assertFalse(url.isExpired());
    }

    @Test
    void testUrlWithFutureExpiresAtNotExpired() {
        Instant futureTime = Instant.now().plusSeconds(3600);
        ShortUrl url = new ShortUrl(1L, "abc1234", "https://example.com", futureTime);
        assertFalse(url.isExpired());
    }

    @Test
    void testUrlWithPastExpiresAtIsExpired() {
        Instant pastTime = Instant.now().minusSeconds(3600);
        ShortUrl url = new ShortUrl(1L, "abc1234", "https://example.com", pastTime);
        assertTrue(url.isExpired());
    }

    @Test
    void testUrlExpiringInOneSecondNotExpired() {
        Instant soonTime = Instant.now().plusSeconds(1);
        ShortUrl url = new ShortUrl(1L, "abc1234", "https://example.com", soonTime);
        assertFalse(url.isExpired());
    }

    @Test
    void testConvenienceConstructorWithTwoArguments() {
        ShortUrl url = new ShortUrl("abc1234", "https://example.com");
        assertNull(url.id());
        assertEquals("abc1234", url.code());
        assertEquals("https://example.com", url.originalUrl());
        assertNull(url.expiresAt());
        assertFalse(url.isExpired());
    }

    @Test
    void testConvenienceConstructorWithThreeArguments() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        ShortUrl url = new ShortUrl("abc1234", "https://example.com", expiresAt);
        assertNull(url.id());
        assertEquals("abc1234", url.code());
        assertEquals("https://example.com", url.originalUrl());
        assertEquals(expiresAt, url.expiresAt());
        assertFalse(url.isExpired());
    }
}
