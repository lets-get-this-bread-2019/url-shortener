package com.example.urlshortener.service;

import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.repository.UrlRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Set;

@Service
public class UrlService {

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 10;

    // Reserved codes that cannot be used as custom short codes
    private static final Set<String> RESERVED_CODES = Set.of(
        "api", "shorten", "health", "admin", "static", "public", "assets"
    );

    private final UrlRepository urlRepository;
    private final SecureRandom random = new SecureRandom();

    public UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    public ShortUrl createShortUrl(String originalUrl) {
        return createShortUrl(originalUrl, null);
    }

    public ShortUrl createShortUrl(String originalUrl, String customCode) {
        // Security: Validate URL scheme to prevent open redirect attacks
        validateUrl(originalUrl);

        if (customCode != null && !customCode.isBlank()) {
            // Validate custom code
            validateCustomCode(customCode);

            // Check if code already exists
            if (urlRepository.findByCode(customCode).isPresent()) {
                throw new CodeAlreadyExistsException("Short code already exists: " + customCode);
            }

            // Use custom code
            return urlRepository.save(new ShortUrl(customCode, originalUrl));
        } else {
            // Generate a unique random code
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                String code = generateRandomCode();
                if (urlRepository.findByCode(code).isEmpty()) {
                    return urlRepository.save(new ShortUrl(code, originalUrl));
                }
            }
            throw new RuntimeException("Failed to generate a unique short code after " + MAX_ATTEMPTS + " attempts");
        }
    }

    public Optional<ShortUrl> findByCode(String code) {
        return urlRepository.findByCode(code);
    }

    /**
     * Validates the URL scheme to prevent open redirect and XSS attacks.
     * Only allows http and https schemes.
     *
     * @param url The URL to validate
     * @throws InvalidUrlException if the URL has an invalid or dangerous scheme
     */
    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL cannot be empty");
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();

            // Scheme is required
            if (scheme == null || scheme.isBlank()) {
                throw new InvalidUrlException("URL must include a scheme (http:// or https://)");
            }

            // Only allow http and https schemes
            String lowerScheme = scheme.toLowerCase();
            if (!lowerScheme.equals("http") && !lowerScheme.equals("https")) {
                throw new InvalidUrlException("Only http and https URLs are allowed. Scheme '" + scheme + "' is not permitted");
            }

            // Ensure the URL has a host
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidUrlException("URL must include a valid host");
            }

        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Invalid URL format: " + e.getMessage());
        }
    }

    private void validateCustomCode(String code) {
        // Length validation: 3-20 characters
        if (code.length() < 3 || code.length() > 20) {
            throw new InvalidCodeException("Custom code must be between 3 and 20 characters");
        }

        // Character validation: alphanumeric only
        if (!code.matches("[a-zA-Z0-9]+")) {
            throw new InvalidCodeException("Custom code must contain only alphanumeric characters");
        }

        // Reserved code check (case-insensitive)
        if (RESERVED_CODES.contains(code.toLowerCase())) {
            throw new InvalidCodeException("Code is reserved and cannot be used: " + code);
        }
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(BASE62_CHARS.charAt(random.nextInt(BASE62_CHARS.length())));
        }
        return sb.toString();
    }

    // Custom exceptions
    public static class InvalidCodeException extends RuntimeException {
        public InvalidCodeException(String message) {
            super(message);
        }
    }

    public static class CodeAlreadyExistsException extends RuntimeException {
        public CodeAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class InvalidUrlException extends RuntimeException {
        public InvalidUrlException(String message) {
            super(message);
        }
    }
}
