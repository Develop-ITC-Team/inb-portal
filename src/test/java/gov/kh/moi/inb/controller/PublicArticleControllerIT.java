package gov.kh.moi.inb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.kh.moi.inb.entity.Article;
import gov.kh.moi.inb.entity.ArticleStatus;
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

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack slice test (controller -> service -> repository -> H2) confirming that:
 * <ul>
 *   <li>Public endpoints are reachable without authentication.</li>
 *   <li>DRAFT articles are never returned by public endpoints, even by direct slug lookup.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicArticleControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        categoryRepository.deleteAll();
        category = categoryRepository.save(Category.builder()
                .name("National News").slug("national-news").active(true).displayOrder(1).build());
    }

    @Test
    void listPublishedArticles_returnsOnlyPublishedOnes() throws Exception {
        articleRepository.save(Article.builder()
                .category(category).title("Published Article").slug("published-article")
                .content("Body").status(ArticleStatus.PUBLISHED).publishedAt(OffsetDateTime.now())
                .build());
        articleRepository.save(Article.builder()
                .category(category).title("Draft Article").slug("draft-article")
                .content("Body").status(ArticleStatus.DRAFT)
                .build());

        mockMvc.perform(get("/api/public/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].slug", is("published-article")));
    }

    @Test
    void getBySlug_returns404_forDraftArticle() throws Exception {
        articleRepository.save(Article.builder()
                .category(category).title("Draft Article").slug("draft-article")
                .content("Body").status(ArticleStatus.DRAFT)
                .build());

        mockMvc.perform(get("/api/public/articles/draft-article"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void adminEndpoint_rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/admin/articles"))
                .andExpect(status().isUnauthorized());
    }
}
