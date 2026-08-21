package gov.kh.moi.inb.dto.response;

import gov.kh.moi.inb.entity.ArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/** Full article representation, used for the detail page. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponseDTO {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String authorName;
    private ArticleStatus status;
    private boolean featured;
    private Long viewCount;
    private OffsetDateTime publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private CategoryResponseDTO category;
    private List<ArticleMediaDTO> media;
}