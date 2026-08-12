package gov.kh.moi.inb.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleRequestDTO {

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    /** Optional - auto-generated from {@code title} via {@code SlugUtil} when omitted. */
    @Size(max = 500, message = "Slug must not exceed 500 characters")
    private String slug;

    @Size(max = 1000, message = "Excerpt must not exceed 1000 characters")
    private String excerpt;

    @NotBlank(message = "Content is required")
    private String content;

    @Size(max = 255, message = "Author name must not exceed 255 characters")
    private String authorName;

    private Boolean featured;

    @Valid
    private List<ArticleMediaRequestDTO> media;
}
