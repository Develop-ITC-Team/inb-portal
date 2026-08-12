package gov.kh.moi.inb.mapper;

import gov.kh.moi.inb.dto.response.CategoryResponseDTO;
import gov.kh.moi.inb.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CategoryMapper {

    /**
     * Note: intentionally the ONLY {@code Category -> CategoryResponseDTO} method here.
     * MapStruct requires a single unambiguous method per (source, target) type pair so it can
     * auto-resolve the nested {@code category} field when {@code ArticleMapper} maps an
     * {@code Article} - a second overload with the same signature would fail annotation
     * processing with an "ambiguous mapping methods" error. The {@code children} tree is built
     * manually in {@code CategoryServiceImpl} where recursion depth needs to be controlled.
     */
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", ignore = true)
    CategoryResponseDTO toDto(Category category);

    List<CategoryResponseDTO> toDtoList(List<Category> categories);
}
