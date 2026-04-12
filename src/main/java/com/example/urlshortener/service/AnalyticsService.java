package com.example.urlshortener.service;

import com.example.urlshortener.model.ClickAnalytics;
import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.repository.ClickAnalyticsRepository;
import com.example.urlshortener.repository.UrlRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final UrlRepository urlRepository;

    public AnalyticsService(ClickAnalyticsRepository clickAnalyticsRepository, UrlRepository urlRepository) {
        this.clickAnalyticsRepository = clickAnalyticsRepository;
        this.urlRepository = urlRepository;
    }

    /**
     * Records a click event for a short URL with privacy-preserving IP hashing.
     *
     * @param shortUrlId The ID of the short URL
     * @param ipAddress The visitor's IP address (will be hashed)
     * @param userAgent The visitor's user agent
     * @param referrer The referrer URL (may be null)
     */
    public void recordClick(Long shortUrlId, String ipAddress, String userAgent, String referrer) {
        String ipHash = hashIpAddress(ipAddress);
        ClickAnalytics click = new ClickAnalytics(shortUrlId, ipHash, userAgent, referrer);
        clickAnalyticsRepository.save(click);
    }

    /**
     * Gets analytics for a given short code.
     *
     * @param code The short code
     * @return Analytics data or empty optional if code not found
     */
    public Optional<AnalyticsData> getAnalytics(String code) {
        Optional<ShortUrl> shortUrlOpt = urlRepository.findByCode(code);
        if (shortUrlOpt.isEmpty()) {
            return Optional.empty();
        }

        ShortUrl shortUrl = shortUrlOpt.get();
        List<ClickAnalytics> clicks = clickAnalyticsRepository.findByShortUrlId(shortUrl.id());

        long totalClicks = clicks.size();
        long uniqueIps = clicks.stream()
            .map(ClickAnalytics::ipHash)
            .distinct()
            .count();

        // Group clicks by date
        Map<LocalDate, Long> clicksByDate = clicks.stream()
            .collect(Collectors.groupingBy(
                click -> LocalDate.ofInstant(click.clickedAt(), ZoneId.systemDefault()),
                Collectors.counting()
            ));

        // Get top referrers (excluding null/empty)
        Map<String, Long> topReferrers = clicks.stream()
            .map(ClickAnalytics::referrer)
            .filter(ref -> ref != null && !ref.isBlank())
            .collect(Collectors.groupingBy(ref -> ref, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        return Optional.of(new AnalyticsData(
            code,
            shortUrl.originalUrl(),
            totalClicks,
            uniqueIps,
            clicksByDate,
            topReferrers
        ));
    }

    /**
     * Hashes an IP address using SHA-256 for privacy.
     * This prevents storing raw IP addresses while still allowing unique visitor counting.
     *
     * @param ipAddress The IP address to hash
     * @return The hashed IP address as a hex string
     */
    private String hashIpAddress(String ipAddress) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ipAddress.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Analytics data transfer object.
     */
    public record AnalyticsData(
        String code,
        String originalUrl,
        long totalClicks,
        long uniqueIps,
        Map<LocalDate, Long> clicksByDate,
        Map<String, Long> topReferrers
    ) {}
}
