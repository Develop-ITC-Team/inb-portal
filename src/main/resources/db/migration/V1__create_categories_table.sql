-- =====================================================================================
-- V1: categories
-- Content taxonomy / navigation nodes. Self-referencing to support nested dropdown menus.
-- =====================================================================================
CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL,
    description     TEXT,
    parent_id       BIGINT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT uq_category_slug UNIQUE (slug)
);

CREATE INDEX idx_category_parent ON categories (parent_id);
CREATE INDEX idx_category_deleted ON categories (deleted);
CREATE INDEX idx_category_active_order ON categories (is_active, display_order);

COMMENT ON TABLE categories IS 'Content categories / navigation nodes for the INB public portal';
COMMENT ON COLUMN categories.slug IS 'URL-friendly unique identifier used in public routes';
