-- =====================================================================================
-- V3: article_media
-- Gallery images/videos/documents attached to an article (one-to-many).
-- =====================================================================================
CREATE TABLE article_media (
    id              BIGSERIAL PRIMARY KEY,
    article_id      BIGINT NOT NULL,
    file_url        VARCHAR(1000) NOT NULL,
    media_type      VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    caption         VARCHAR(500),
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_cover        BOOLEAN NOT NULL DEFAULT FALSE,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_article_media_article
        FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT chk_article_media_type
        CHECK (media_type IN ('IMAGE', 'VIDEO', 'DOCUMENT'))
);

CREATE INDEX idx_article_media_article ON article_media (article_id);
CREATE UNIQUE INDEX uq_article_media_single_cover
    ON article_media (article_id)
    WHERE is_cover = TRUE;

COMMENT ON TABLE article_media IS 'Gallery media assets attached to an article';
COMMENT ON COLUMN article_media.file_url IS 'Opaque URL - local disk today, MinIO later; consumers never construct paths themselves';
