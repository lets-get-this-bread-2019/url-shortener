package com.example.urlshortener.service;

import com.example.urlshortener.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UrlCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(UrlCleanupService.class);

    private final UrlRepository urlRepository;

    public UrlCleanupService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    /**
     * Scheduled cleanup job that runs every hour to delete expired URLs.
     * This helps manage database size and remove outdated short links.
     */
    @Scheduled(cron = "0 0 * * * *") // Run at the start of every hour
    public void cleanupExpiredUrls() {
        logger.info("Starting cleanup of expired URLs");

        try {
            int deletedCount = urlRepository.deleteExpiredUrls(Instant.now());

            if (deletedCount > 0) {
                logger.info("Cleaned up {} expired URL(s)", deletedCount);
            } else {
                logger.debug("No expired URLs to clean up");
            }
        } catch (Exception e) {
            logger.error("Error during URL cleanup", e);
        }
    }
}
