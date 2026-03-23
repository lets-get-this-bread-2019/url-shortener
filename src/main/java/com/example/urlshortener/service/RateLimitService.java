package com.example.urlshortener.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Bucket> shortenBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> redirectBuckets = new ConcurrentHashMap<>();

    /**
     * Checks if a request to the shorten endpoint from the given IP is allowed.
     *
     * @param ip The client IP address
     * @param requestsPerMinute The rate limit threshold
     * @return true if the request is allowed, false if rate limit exceeded
     */
    public boolean allowShortenRequest(String ip, int requestsPerMinute) {
        Bucket bucket = shortenBuckets.computeIfAbsent(ip, k -> createBucket(requestsPerMinute));
        return bucket.tryConsume(1);
    }

    /**
     * Checks if a request to the redirect endpoint from the given IP is allowed.
     *
     * @param ip The client IP address
     * @param requestsPerMinute The rate limit threshold
     * @return true if the request is allowed, false if rate limit exceeded
     */
    public boolean allowRedirectRequest(String ip, int requestsPerMinute) {
        Bucket bucket = redirectBuckets.computeIfAbsent(ip, k -> createBucket(requestsPerMinute));
        return bucket.tryConsume(1);
    }

    /**
     * Gets the number of seconds until the next token is available for the shorten endpoint.
     *
     * @param ip The client IP address
     * @param requestsPerMinute The rate limit threshold
     * @return seconds until next token is available
     */
    public long getSecondsUntilShortenRefill(String ip, int requestsPerMinute) {
        Bucket bucket = shortenBuckets.computeIfAbsent(ip, k -> createBucket(requestsPerMinute));
        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
    }

    /**
     * Gets the number of seconds until the next token is available for the redirect endpoint.
     *
     * @param ip The client IP address
     * @param requestsPerMinute The rate limit threshold
     * @return seconds until next token is available
     */
    public long getSecondsUntilRedirectRefill(String ip, int requestsPerMinute) {
        Bucket bucket = redirectBuckets.computeIfAbsent(ip, k -> createBucket(requestsPerMinute));
        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
    }

    /**
     * Creates a new bucket with the specified requests per minute limit.
     *
     * @param requestsPerMinute The maximum number of requests allowed per minute
     * @return A new Bucket instance
     */
    private Bucket createBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.classic(
            requestsPerMinute,
            Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
}
