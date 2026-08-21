package gov.kh.moi.inb.dto.response;

import gov.kh.moi.inb.entity.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleMediaDTO {
    private Long id;
    private String fileUrl;
    private MediaType mediaType;
    private String caption;
    private Integer displayOrder;
    private boolean cover;
}
