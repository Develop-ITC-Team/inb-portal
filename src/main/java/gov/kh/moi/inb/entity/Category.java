package gov.kh.moi.inb.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A content category / navigation node (e.g. "ព័ត៌មានជាតិ" - National News,
 * "សកម្មភាពថ្នាក់ដឹកនាំ" - Leadership Activities).
 *
 * <p>Self-referencing via {@link #parent} to support the nested dropdown navigation observed
 * in the UI (អំពីយើង / ព័ត៌មាន dropdowns), without needing a separate Menu entity for the
 * first iteration.
 */
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_slug", columnList = "slug", unique = true),
        @Index(name = "idx_category_parent", columnList = "parent_id"),
        @Index(name = "idx_category_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"parent", "children"})
@EqualsAndHashCode(callSuper = false, of = "id")
public class Category extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    /** URL-friendly unique identifier, e.g. "national-news". Used in public routes. */
    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Category> children = new ArrayList<>();

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
