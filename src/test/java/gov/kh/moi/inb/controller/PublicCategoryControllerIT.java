package gov.kh.moi.inb.controller;

import gov.kh.moi.inb.entity.Category;
import gov.kh.moi.inb.repository.ArticleRepository;
import gov.kh.moi.inb.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the public (unauthenticated) category endpoints - the navigation tree
 * the citizen-facing site renders its menu from, and direct slug lookups.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicCategoryControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ArticleRepository articleRepository;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void getCategoryTree_excludesInactiveRootCategories() throws Exception {
        categoryRepository.save(Category.builder()
                .name("Visible Root").slug("visible-root").active(true).displayOrder(1).build());
        categoryRepository.save(Category.builder()
                .name("Hidden Root").slug("hidden-root").active(false).displayOrder(2).build());

        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(1)))
                .andExpect(jsonPath("$.data[0].slug", is("visible-root")));
    }

    @Test
    void getCategoryTree_excludesInactiveAndDeletedChildren() throws Exception {
        Category parent = categoryRepository.save(Category.builder()
                .name("Parent").slug("parent").active(true).displayOrder(1).build());
        categoryRepository.save(Category.builder()
                .name("Active Child").slug("active-child").parent(parent).active(true).displayOrder(1).build());
        categoryRepository.save(Category.builder()
                .name("Inactive Child").slug("inactive-child").parent(parent).active(false).displayOrder(2).build());
        Category deletedChild = categoryRepository.save(Category.builder()
                .name("Deleted Child").slug("deleted-child").parent(parent).active(true).displayOrder(3).build());
        deletedChild.setDeleted(true);
        categoryRepository.save(deletedChild);

        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].children.length()", is(1)))
                .andExpect(jsonPath("$.data[0].children[0].slug", is("active-child")));
    }

    @Test
    void getBySlug_returnsCategory_forExistingSlug() throws Exception {
        categoryRepository.save(Category.builder()
                .name("National News").slug("national-news").active(true).displayOrder(1).build());

        mockMvc.perform(get("/api/public/categories/{slug}", "national-news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("National News")));
    }

    @Test
    void getBySlug_returns404_forUnknownSlug() throws Exception {
        mockMvc.perform(get("/api/public/categories/{slug}", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void getBySlug_returns404_forSoftDeletedCategory() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .name("Gone Category").slug("gone-category").active(true).displayOrder(1).build());
        category.setDeleted(true);
        categoryRepository.save(category);

        mockMvc.perform(get("/api/public/categories/{slug}", "gone-category"))
                .andExpect(status().isNotFound());
    }
}
