package com.example.urlshortener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper utilities for tests, particularly for rate limiting scenarios.
 */
public class TestHelper {
    private static final AtomicInteger ipCounter = new AtomicInteger(0);

    /**
     * Generates a unique test IP address for each test to avoid rate limit conflicts.
     */
    public static String uniqueTestIp() {
        return "10.0.0." + ipCounter.incrementAndGet();
    }
}
