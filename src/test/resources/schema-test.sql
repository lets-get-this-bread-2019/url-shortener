CREATE TABLE IF NOT EXISTS short_urls (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  code       VARCHAR(20) NOT NULL UNIQUE,
  original_url VARCHAR(2048) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_short_urls_expires_at ON short_urls(expires_at);
