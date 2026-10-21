package gov.kh.moi.inb.service;

import gov.kh.moi.inb.dto.request.CategoryRequestDTO;
import gov.kh.moi.inb.dto.response.CategoryResponseDTO;
import gov.kh.moi.inb.entity.Category;
import gov.kh.moi.inb.exception.ResourceNotFoundException;
import gov.kh.moi.inb.mapper.CategoryMapper;
import gov.kh.moi.inb.repository.CategoryRepository;
import gov.kh.moi.inb.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the core category business rules: slug generation/uniqueness, the
 * self-parenting guard, the "can't delete a category with children" guard, and not-found
 * handling for soft-deleted records - mirroring {@link ArticleServiceImplTest} for the
 * category resource, which previously had no equivalent fast unit-level coverage of its own
 * (only indirectly, through {@code AdminCategoryControllerIT}).
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void create_generatesSlugFromName_whenSlugNotProvided() {
        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("National News");

        when(categoryRepository.existsBySlugAndDeletedFalse("national-news")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryMapper.toDto(any(Category.class))).thenReturn(CategoryResponseDTO.builder().build());

        categoryService.create(request, "admin1");

        var captor = org.mockito.ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("national-news");
    }

    @Test
    void create_appendsSuffix_whenSlugAlreadyExists() {
        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("National News");

        when(categoryRepository.existsBySlugAndDeletedFalse("national-news")).thenReturn(true);
        when(categoryRepository.existsBySlugAndDeletedFalse("national-news-2")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryMapper.toDto(any(Category.class))).thenReturn(CategoryResponseDTO.builder().build());

        categoryService.create(request, "admin1");

        var captor = org.mockito.ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("national-news-2");
    }

    @Test
    void update_throwsIllegalArgument_whenSettingSelfAsParent() {
        Category existing = Category.builder().id(1L).name("Loop").slug("loop").active(true).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setParentId(1L);

        assertThatThrownBy(() -> categoryService.update(1L, request, "admin1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be its own parent");
    }

    @Test
    void delete_throwsIllegalArgument_whenCategoryHasChildren() {
        Category parent = Category.builder().id(1L).name("Parent").slug("parent").active(true)
                .children(List.of(Category.builder().id(2L).name("Child").slug("child").build()))
                .build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> categoryService.delete(1L, "admin1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("child categories");
    }

    @Test
    void delete_softDeletes_whenNoChildren() {
        Category leaf = Category.builder().id(3L).name("Leaf").slug("leaf").active(true)
                .children(List.of())
                .build();
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(leaf));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.delete(3L, "admin1");

        var captor = org.mockito.ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
    }

    @Test
    void getById_throwsNotFound_whenCategoryIsSoftDeleted() {
        Category deleted = Category.builder().id(9L).name("Gone").slug("gone").build();
        deleted.setDeleted(true);
        when(categoryRepository.findById(9L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> categoryService.getById(9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPublicCategoryTree_excludesInactiveAndSoftDeletedChildren() {
        Category activeChild = Category.builder().id(2L).name("Active").slug("active").active(true).build();
        Category inactiveChild = Category.builder().id(3L).name("Inactive").slug("inactive").active(false).build();
        Category deletedChild = Category.builder().id(4L).name("Deleted").slug("deleted").active(true).build();
        deletedChild.setDeleted(true);

        Category root = Category.builder().id(1L).name("Root").slug("root").active(true)
                .children(List.of(activeChild, inactiveChild, deletedChild))
                .build();

        when(categoryRepository.findRootCategories()).thenReturn(List.of(root));
        when(categoryMapper.toDto(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            return CategoryResponseDTO.builder().id(c.getId()).name(c.getName()).slug(c.getSlug())
                    .active(c.isActive()).build();
        });

        List<CategoryResponseDTO> tree = categoryService.getPublicCategoryTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren()).extracting(CategoryResponseDTO::getSlug)
                .containsExactly("active");
    }
}
