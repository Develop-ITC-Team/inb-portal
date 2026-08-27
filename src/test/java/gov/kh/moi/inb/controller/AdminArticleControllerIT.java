package gov.kh.moi.inb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminArticleControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    // Must match app.jwt.secret in src/test/resources/application-test.yml exactly.
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private Category category;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        categoryRepository.deleteAll();
        category = categoryRepository.save(Category.builder()
                .name("Announcements").slug("announcements").active(true).displayOrder(1).build());
    }

    /** Generates a valid, correctly-signed JWT for the given username and roles, on the fly. */
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
    void createArticle_succeeds_withValidAdminToken() throws Exception {
        String token = generateToken("editor1", "ADMIN", "EDITOR");

        Map<String, Object> request = Map.of(
                "categoryId", category.getId(),
                "title", "Test Announcement",
                "excerpt", "A short excerpt.",
                "content", "<p>Full body content.</p>",
                "authorName", "System Admin"
        );

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.title", is("Test Announcement")));
    }

    @Test
    void createArticle_rejected_withoutToken() throws Exception {
        Map<String, Object> request = Map.of(
                "categoryId", category.getId(),
                "title", "Should Not Be Created",
                "content", "<p>Body</p>"
        );

        mockMvc.perform(post("/api/admin/articles")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createArticle_rejected_forEditorOnlyRole_onDeleteEndpoint() throws Exception {

        String editorOnlyToken = generateToken("editor2", "EDITOR");

        mockMvc.perform(delete("/api/admin/articles/999")
                        .header("Authorization", "Bearer " + editorOnlyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void fullPublishWorkflow_createThenPublishThenVisiblePublicly() throws Exception {
        String token = generateToken("editor1", "ADMIN", "EDITOR");

        Map<String, Object> createRequest = Map.of(
                "categoryId", category.getId(),
                "title", "Workflow Test Article",
                "content", "<p>Body</p>"
        );

        String createResponse = mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long articleId = ((Number) objectMapper.readTree(createResponse)
                .path("data").path("id").numberValue()).longValue();

        // Not visible publicly yet - still DRAFT.
        mockMvc.perform(get("/api/public/articles/{slug}", "workflow-test-article"))
                .andExpect(status().isNotFound());

        // Publish it.
        mockMvc.perform(patch("/api/admin/articles/{id}/status", articleId)
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.data.publishedAt").exists());

        // Now visible publicly.
        mockMvc.perform(get("/api/public/articles/{slug}", "workflow-test-article"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PUBLISHED")));
    }
}
