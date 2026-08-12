package gov.kh.moi.inb.entity;

/**
 * Editorial workflow state of an {@link Article}.
 * Only PUBLISHED articles are ever visible through public read endpoints.
 */
public enum ArticleStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
