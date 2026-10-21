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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Edge-case and input-validation tests for article creation - specifically checking how the
 * API responds to bad input, not just the happy path. A public-facing government API must fail
 * predictably (clean 400/404 with a useful message) rather than leaking a stack trace or
 * returning a generic 500 for input errors a client could reasonably send.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArticleEdgeCaseIT {

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

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        categoryRepository.deleteAll();
        category = categoryRepository.save(Category.builder()
                .name("Announcements").slug("announcements").active(true).displayOrder(1).build());
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
    void createArticle_autoUniquifiesSlug_onDuplicateTitle() throws Exception {
        String token = generateToken("editor1", "ADMIN", "EDITOR");
        Map<String, Object> request = Map.of(
                "categoryId", category.getId(),
                "title", "Same Title Twice",
                "content", "<p>Body</p>"
        );

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug", is("same-title-twice")));

        // A second article with the identical title must not collide on the unique slug
        // constraint - the service must auto-suffix it instead of failing.
        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug", is("same-title-twice-2")));
    }

    @Test
    void createArticle_rejected_withNonExistentCategoryId() throws Exception {
        String token = generateToken("editor1", "ADMIN", "EDITOR");
        Map<String, Object> request = Map.of(
                "categoryId", 999999L,
                "title", "Orphan Article",
                "content", "<p>Body</p>"
        );

        // A category ID that doesn't exist must fail cleanly (404), not throw an
        // unhandled foreign-key constraint violation up as a 500.
        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void createArticle_rejected_withMissingTitle() throws Exception {
        String token = generateToken("editor1", "ADMIN", "EDITOR");
        Map<String, Object> request = new HashMap<>();
        request.put("categoryId", category.getId());
        request.put("content", "<p>Body with no title at all</p>");
        // "title" intentionally omitted - @NotBlank on ArticleRequestDTO.title should catch this.

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.violations", notNullValue()));
    }

    @Test
    void createArticle_rejected_withMissingContent() throws Exception {
        String token = generateToken("editor1", "ADMIN", "EDITOR");
        Map<String, Object> request = Map.of(
                "categoryId", category.getId(),
                "title", "Title With No Body"
                // "content" intentionally omitted - required per ArticleRequestDTO.
        );

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void createArticle_rejected_withMissingCategoryId() throws Exception {
        String token = generateToken("editor1", "ADMIN", "EDITOR");
        Map<String, Object> request = Map.of(
                "title", "No Category At All",
                "content", "<p>Body</p>"
                // "categoryId" intentionally omitted - @NotNull on ArticleRequestDTO.
        );

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createArticle_rejected_withTitleExceedingMaxLength() throws Exception {
        String token = generateToken("editor1", "ADMIN", "EDITOR");
        // ArticleRequestDTO.title is capped at 500 chars - this deliberately exceeds it.
        String oversizedTitle = "A".repeat(501);
        Map<String, Object> request = Map.of(
                "categoryId", category.getId(),
                "title", oversizedTitle,
                "content", "<p>Body</p>"
        );

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createArticle_rejected_withMalformedJson() throws Exception {
        String token = generateToken("editor1", "ADMIN", "EDITOR");

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{ this is not valid json "))
                .andExpect(status().isBadRequest());
    }
}
