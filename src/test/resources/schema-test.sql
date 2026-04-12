CREATE TABLE IF NOT EXISTS short_urls (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  code       VARCHAR(20) NOT NULL UNIQUE,
  original_url VARCHAR(2048) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_short_urls_expires_at ON short_urls(expires_at);

-- Click analytics table for tracking URL clicks
CREATE TABLE IF NOT EXISTS click_analytics (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  short_url_id BIGINT NOT NULL REFERENCES short_urls(id) ON DELETE CASCADE,
  clicked_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  ip_hash     VARCHAR(64) NOT NULL,
  user_agent  VARCHAR(512),
  referrer    VARCHAR(2048)
);

-- Indexes for efficient analytics queries
CREATE INDEX IF NOT EXISTS idx_click_analytics_short_url_id ON click_analytics(short_url_id);
CREATE INDEX IF NOT EXISTS idx_click_analytics_clicked_at ON click_analytics(clicked_at);
