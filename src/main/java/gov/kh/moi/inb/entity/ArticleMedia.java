package gov.kh.moi.inb.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * A single media asset (image/video/document) attached to an {@link Article}, supporting the
 * multi-image gallery pattern seen in the UI ("+14", "+4", "+8" thumbnail overlays).
 *
 * <p>{@code fileUrl} currently points at this service's local static file endpoint
 * ({@code /files/**}); when the MinIO object-storage service is introduced, only
 * {@code FileStorageService}'s implementation changes — this entity and every consumer of it
 * are unaffected because they only ever deal with an opaque URL string.
 */
@Entity
@Table(name = "article_media", indexes = {
        @Index(name = "idx_article_media_article", columnList = "article_id")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = "article")
@EqualsAndHashCode(callSuper = false, of = "id")
public class ArticleMedia extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType = MediaType.IMAGE;

    @Column(length = 500)
    private String caption;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /** Marks the single image used as the card/hero thumbnail for this article. */
    @Builder.Default
    @Column(name = "is_cover", nullable = false)
    private boolean cover = false;
}
