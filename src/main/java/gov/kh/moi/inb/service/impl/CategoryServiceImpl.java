package gov.kh.moi.inb.service.impl;

import gov.kh.moi.inb.dto.request.CategoryRequestDTO;
import gov.kh.moi.inb.dto.response.CategoryResponseDTO;
import gov.kh.moi.inb.entity.Category;
import gov.kh.moi.inb.exception.ResourceNotFoundException;
import gov.kh.moi.inb.mapper.CategoryMapper;
import gov.kh.moi.inb.repository.CategoryRepository;
import gov.kh.moi.inb.service.CategoryService;
import gov.kh.moi.inb.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponseDTO> getPublicCategoryTree() {
        List<Category> roots = categoryRepository.findRootCategories().stream()
                .filter(Category::isActive)
                .toList();
        return roots.stream().map(this::toTreeDto).collect(Collectors.toList());
    }

    private CategoryResponseDTO toTreeDto(Category category) {
        CategoryResponseDTO dto = categoryMapper.toDto(category);
        List<CategoryResponseDTO> children = category.getChildren().stream()
                .filter(c -> c.isActive() && !c.isDeleted())
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                .map(this::toTreeDto)
                .toList();
        // CategoryResponseDTO is immutable (Lombok @Builder over @Getter); rebuild with children.
        return CategoryResponseDTO.builder()
                .id(dto.getId())
                .name(dto.getName())
                .slug(dto.getSlug())
                .description(dto.getDescription())
                .parentId(dto.getParentId())
                .displayOrder(dto.getDisplayOrder())
                .active(dto.isActive())
                .children(children)
                .build();
    }

    @Override
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryMapper.toDtoList(categoryRepository.findAllByDeletedFalseOrderByDisplayOrderAsc());
    }

    @Override
    public CategoryResponseDTO getBySlug(String slug) {
        Category category = categoryRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", slug));
        return categoryMapper.toDto(category);
    }

    @Override
    public CategoryResponseDTO getById(Long id) {
        return categoryMapper.toDto(findActiveById(id));
    }

    @Override
    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO request, String actor) {
        String slug = resolveSlug(request.getSlug(), request.getName(), null);

        Category parent = null;
        if (request.getParentId() != null) {
            parent = findActiveById(request.getParentId());
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .parent(parent)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() == null || request.getActive())
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: id={}, slug={}, by={}", saved.getId(), saved.getSlug(), actor);
        return categoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CategoryResponseDTO update(Long id, CategoryRequestDTO request, String actor) {
        Category category = findActiveById(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            category.setName(request.getName());
        }
        String slug = resolveSlug(request.getSlug(), category.getName(), id);
        category.setSlug(slug);
        category.setDescription(request.getDescription());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            category.setParent(findActiveById(request.getParentId()));
        } else {
            category.setParent(null);
        }

        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category saved = categoryRepository.save(category);
        log.info("Category updated: id={}, by={}", saved.getId(), actor);
        return categoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, String actor) {
        Category category = findActiveById(id);
        if (!category.getChildren().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete a category that has child categories");
        }
        category.setDeleted(true);
        categoryRepository.save(category);
        log.info("Category soft-deleted: id={}, by={}", id, actor);
    }

    private Category findActiveById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        if (category.isDeleted()) {
            throw ResourceNotFoundException.of("Category", id);
        }
        return category;
    }

    private String resolveSlug(String providedSlug, String fallbackSource, Long excludeId) {
        String base = (providedSlug != null && !providedSlug.isBlank())
                ? SlugUtil.toSlug(providedSlug)
                : SlugUtil.toSlug(fallbackSource);

        if (base.isBlank()) {
            throw new IllegalArgumentException("Unable to derive a valid slug from the provided name");
        }

        String candidate = base;
        int suffix = 1;
        while (excludeId == null
                ? categoryRepository.existsBySlugAndDeletedFalse(candidate)
                : categoryRepository.existsBySlugAndIdNotAndDeletedFalse(candidate, excludeId)) {
            suffix++;
            candidate = base + "-" + suffix;
        }
        return candidate;
    }
}
