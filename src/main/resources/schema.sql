CREATE TABLE IF NOT EXISTS short_urls (
  id        BIGSERIAL PRIMARY KEY,
  code      TEXT NOT NULL UNIQUE,
  original_url TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMPTZ
);

-- Index for efficient cleanup of expired URLs
CREATE INDEX IF NOT EXISTS idx_short_urls_expires_at ON short_urls(expires_at) WHERE expires_at IS NOT NULL;
