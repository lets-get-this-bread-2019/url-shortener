package com.example.urlshortener.repository;

import com.example.urlshortener.model.ClickAnalytics;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickAnalyticsRepository extends CrudRepository<ClickAnalytics, Long> {

    @Query("SELECT * FROM click_analytics WHERE short_url_id = :shortUrlId ORDER BY clicked_at DESC")
    List<ClickAnalytics> findByShortUrlId(Long shortUrlId);

    @Query("SELECT COUNT(*) FROM click_analytics WHERE short_url_id = :shortUrlId")
    long countByShortUrlId(Long shortUrlId);

    @Query("SELECT COUNT(DISTINCT ip_hash) FROM click_analytics WHERE short_url_id = :shortUrlId")
    long countUniqueIpsByShortUrlId(Long shortUrlId);
}
