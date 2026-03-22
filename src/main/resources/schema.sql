-- schema.sql
CREATE TABLE IF NOT EXISTS short_urls (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT NOT NULL UNIQUE,
  original_url TEXT NOT NULL,
  created_at TEXT DEFAULT (datetime('now'))
);
