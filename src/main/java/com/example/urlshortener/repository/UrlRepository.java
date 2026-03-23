package com.example.urlshortener.repository;

import com.example.urlshortener.model.ShortUrl;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UrlRepository extends CrudRepository<ShortUrl, Long> {

    @Query("SELECT * FROM short_urls WHERE code = :code")
    Optional<ShortUrl> findByCode(String code);

    @Modifying
    @Query("DELETE FROM short_urls WHERE expires_at IS NOT NULL AND expires_at < :now")
    int deleteExpiredUrls(Instant now);
}
