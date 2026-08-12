package gov.kh.moi.inb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDTO {

    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    private String name;

    /** Optional - auto-generated from {@code name} via {@code SlugUtil} when omitted. */
    @Size(max = 255, message = "Slug must not exceed 255 characters")
    private String slug;

    private String description;

    /** Null for a top-level category. */
    private Long parentId;

    private Integer displayOrder;

    private Boolean active;
}
