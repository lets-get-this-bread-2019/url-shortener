CREATE TABLE IF NOT EXISTS short_urls (
  id        BIGSERIAL PRIMARY KEY,
  code      TEXT NOT NULL UNIQUE,
  original_url TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMPTZ
);

-- Index for efficient cleanup of expired URLs
CREATE INDEX IF NOT EXISTS idx_short_urls_expires_at ON short_urls(expires_at) WHERE expires_at IS NOT NULL;

-- Click analytics table for tracking URL clicks
CREATE TABLE IF NOT EXISTS click_analytics (
  id          BIGSERIAL PRIMARY KEY,
  short_url_id BIGINT NOT NULL REFERENCES short_urls(id) ON DELETE CASCADE,
  clicked_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ip_hash     TEXT NOT NULL,
  user_agent  TEXT,
  referrer    TEXT
);

-- Indexes for efficient analytics queries
CREATE INDEX IF NOT EXISTS idx_click_analytics_short_url_id ON click_analytics(short_url_id);
CREATE INDEX IF NOT EXISTS idx_click_analytics_clicked_at ON click_analytics(clicked_at);
