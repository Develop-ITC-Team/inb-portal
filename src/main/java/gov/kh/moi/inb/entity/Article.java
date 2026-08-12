package gov.kh.moi.inb.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A news article / announcement published on the public portal.
 *
 * <p>Mirrors the content structure observed on the live site: a category, a title, a short
 * excerpt shown on listing cards, full rich-text body content, an optional image gallery,
 * and an editorial publish workflow ({@link ArticleStatus}).
 */
@Entity
@Table(name = "articles", indexes = {
        @Index(name = "idx_article_slug", columnList = "slug", unique = true),
        @Index(name = "idx_article_category", columnList = "category_id"),
        @Index(name = "idx_article_status_published", columnList = "status, published_at"),
        @Index(name = "idx_article_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"category", "mediaList"})
@EqualsAndHashCode(callSuper = false, of = "id")
public class Article extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 500)
    private String title;

    /** URL-friendly unique identifier used for the public detail-page route. */
    @Column(nullable = false, unique = true, length = 500)
    private String slug;

    /** Short summary shown on listing cards, distinct from the full body. */
    @Column(length = 1000)
    private String excerpt;

    /** Full rich-text (HTML) body content, may contain inline image references. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Public byline. Intentionally a plain string rather than a FK to a User entity: user
     * accounts live in the separate Auth/Identity microservice. {@code createdBy}/{@code
     * updatedBy} (inherited audit fields) track the actual system actor for internal audit
     * purposes; this field is what's shown to the public.
     */
    @Column(name = "author_name", length = 255)
    private String authorName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleStatus status = ArticleStatus.DRAFT;

    @Builder.Default
    @Column(name = "is_featured", nullable = false)
    private boolean featured = false;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Builder.Default
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<ArticleMedia> mediaList = new ArrayList<>();

    /** Convenience helper that keeps both sides of the bidirectional association in sync. */
    public void addMedia(ArticleMedia media) {
        mediaList.add(media);
        media.setArticle(this);
    }

    public void removeMedia(ArticleMedia media) {
        mediaList.remove(media);
        media.setArticle(null);
    }
}
