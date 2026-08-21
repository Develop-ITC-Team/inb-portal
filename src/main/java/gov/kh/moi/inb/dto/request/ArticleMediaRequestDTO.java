package gov.kh.moi.inb.dto.request;

import gov.kh.moi.inb.entity.MediaType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Attaches an already-uploaded file (see {@code POST /api/admin/media/upload}) to an article.
 * Kept separate from the multipart upload step so an article can reference media uploaded
 * independently, and so re-ordering/captioning doesn't require re-uploading the file.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleMediaRequestDTO {

    @NotBlank(message = "fileUrl is required")
    private String fileUrl;

    private MediaType mediaType;

    private String caption;

    private Integer displayOrder;

    private Boolean cover;
}
