package gov.kh.moi.inb.service;

import gov.kh.moi.inb.dto.request.CategoryRequestDTO;
import gov.kh.moi.inb.dto.response.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {

    /** Public: active categories only, nested under their parent. */
    List<CategoryResponseDTO> getPublicCategoryTree();

    /** Admin: all non-deleted categories, flat list. */
    List<CategoryResponseDTO> getAllCategories();

    CategoryResponseDTO getBySlug(String slug);

    CategoryResponseDTO getById(Long id);

    CategoryResponseDTO create(CategoryRequestDTO request, String actor);

    CategoryResponseDTO update(Long id, CategoryRequestDTO request, String actor);

    void delete(Long id, String actor);
}
