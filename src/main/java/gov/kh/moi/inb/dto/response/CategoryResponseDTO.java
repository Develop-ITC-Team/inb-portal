package gov.kh.moi.inb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long parentId;
    private Integer displayOrder;
    private boolean active;
    private List<CategoryResponseDTO> children;
}
