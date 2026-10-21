package gov.kh.moi.inb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.kh.moi.inb.entity.Article;
import gov.kh.moi.inb.entity.ArticleStatus;
import gov.kh.moi.inb.entity.Category;
import gov.kh.moi.inb.repository.ArticleRepository;
import gov.kh.moi.inb.repository.CategoryRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the admin article write/lifecycle endpoints beyond initial creation -
 * update, status change, and soft-delete, including how each behaves against an id that
 * doesn't exist. {@link AdminArticleControllerIT} already covers creation and the full
 * publish workflow; this file fills the remaining gap left on existing resources.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminArticleLifecycleIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private Category category;
    private Article article;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        categoryRepository.deleteAll();
        category = categoryRepository.save(Category.builder()
                .name("Announcements").slug("announcements").active(true).displayOrder(1).build());
        article = articleRepository.save(Article.builder()
                .category(category)
                .title("Original Title")
                .slug("original-title")
                .content("<p>Original body</p>")
                .status(ArticleStatus.DRAFT)
                .featured(false)
                .build());
    }

    private String generateToken(String username, String... roles) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.builder()
                .subject(username)
                .claim("roles", List.of(roles))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    @Test
    void updateArticle_succeeds_withAdminToken() throws Exception {
        String token = generateToken("admin1", "ADMIN");
        Map<String, Object> request = Map.of(
                "categoryId", category.getId(),
                "title", "Updated Title",
                "content", "<p>Updated body</p>"
        );

        mockMvc.perform(put("/api/admin/articles/{id}", article.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("Updated Title")))
                .andExpect(jsonPath("$.data.slug", is("updated-title")));
    }

    @Test
    void updateArticle_rejected_withNonExistentId() throws Exception {
        String token = generateToken("admin1", "ADMIN");
        Map<String, Object> request = Map.of(
                "categoryId", category.getId(),
                "title", "Doesn't Matter",
                "content", "<p>Body</p>"
        );

        mockMvc.perform(put("/api/admin/articles/{id}", 999999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteArticle_succeeds_withAdminToken_andSoftDeletesIt() throws Exception {
        String token = generateToken("admin1", "ADMIN");

        mockMvc.perform(delete("/api/admin/articles/{id}", article.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Soft-deleted articles must behave as gone from the admin side too, not just public.
        mockMvc.perform(get("/api/admin/articles/{id}", article.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteArticle_rejected_withNonExistentId() throws Exception {
        String token = generateToken("admin1", "ADMIN");

        mockMvc.perform(delete("/api/admin/articles/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeStatus_rejected_withNonExistentId() throws Exception {
        String token = generateToken("admin1", "ADMIN");

        mockMvc.perform(patch("/api/admin/articles/{id}/status", 999999L)
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PUBLISHED"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_rejected_withNonExistentId() throws Exception {
        String token = generateToken("admin1", "ADMIN");

        mockMvc.perform(get("/api/admin/articles/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsOnlyArticlesMatchingStatusFilter() throws Exception {
        String token = generateToken("editor1", "EDITOR");
        articleRepository.save(Article.builder()
                .category(category)
                .title("A Published One")
                .slug("a-published-one")
                .content("<p>Body</p>")
                .status(ArticleStatus.PUBLISHED)
                .featured(false)
                .build());

        mockMvc.perform(get("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()", is(1)))
                .andExpect(jsonPath("$.data.content[0].title", is("A Published One")));
    }
}
