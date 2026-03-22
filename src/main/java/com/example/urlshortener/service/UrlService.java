package com.example.urlshortener.service;

import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.repository.UrlRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UrlService {

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 7;

    private final UrlRepository urlRepository;

    public UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    public ShortUrl createShortUrl(String originalUrl) {
        // Save first to get the auto-generated ID
        ShortUrl tempUrl = new ShortUrl("", originalUrl);
        ShortUrl saved = urlRepository.save(tempUrl);

        // Generate code from the ID
        String code = encodeBase62(saved.id());

        // Update with the generated code
        ShortUrl finalUrl = new ShortUrl(saved.id(), code, originalUrl, saved.createdAt());
        return urlRepository.save(finalUrl);
    }

    public Optional<ShortUrl> findByCode(String code) {
        return urlRepository.findByCode(code);
    }

    private String encodeBase62(long id) {
        if (id == 0) {
            return "0".repeat(CODE_LENGTH);
        }

        StringBuilder sb = new StringBuilder();
        long value = id;

        while (value > 0) {
            int remainder = (int) (value % 62);
            sb.insert(0, BASE62_CHARS.charAt(remainder));
            value /= 62;
        }

        // Left-pad to 7 characters
        while (sb.length() < CODE_LENGTH) {
            sb.insert(0, '0');
        }

        return sb.toString();
    }
}
