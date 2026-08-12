package gov.kh.moi.inb.dto.response;

import gov.kh.moi.inb.entity.ArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Lightweight projection used for list/grid views (article cards), deliberately excluding the
 * full {@code content} body to keep listing payloads small - matches the "View" card pattern
 * seen on the news listing page.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSummaryDTO {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String authorName;
    private ArticleStatus status;
    private boolean featured;
    private Long viewCount;
    private OffsetDateTime publishedAt;
    private Instant updatedAt;
    private String categoryName;
    private String categorySlug;
    private String coverImageUrl;
    private int mediaCount;
}