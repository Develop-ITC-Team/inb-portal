-- =====================================================================================
-- V2: articles
-- News articles / announcements, with an editorial publish workflow.
-- =====================================================================================
CREATE TABLE articles (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT NOT NULL,
    title           VARCHAR(500) NOT NULL,
    slug            VARCHAR(500) NOT NULL,
    excerpt         VARCHAR(1000),
    content         TEXT NOT NULL,
    author_name     VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    is_featured     BOOLEAN NOT NULL DEFAULT FALSE,
    view_count      BIGINT NOT NULL DEFAULT 0,
    published_at    TIMESTAMPTZ NULL,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_article_category
        FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT uq_article_slug UNIQUE (slug),
    CONSTRAINT chk_article_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE INDEX idx_article_category ON articles (category_id);
CREATE INDEX idx_article_status_published ON articles (status, published_at DESC);
CREATE INDEX idx_article_deleted ON articles (deleted);
CREATE INDEX idx_article_featured ON articles (is_featured) WHERE is_featured = TRUE;

-- Supports "GET /api/public/articles?search=" without a full Elasticsearch dependency yet.
CREATE INDEX idx_article_title_trgm ON articles USING gin (to_tsvector('simple', title));

COMMENT ON TABLE articles IS 'News articles / announcements published on the INB public portal';
COMMENT ON COLUMN articles.author_name IS 'Public byline (display only) - not a FK; real actor tracked via created_by/updated_by';
