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

/**
 * Integration test for the JWT-protected admin category CRUD endpoints - the last controller
 * in this module to get automated coverage, completing full-stack proof for every admin
 * endpoint (Articles, Media, Categories).
 *
 * <p>Category write operations (create/update/delete) require {@code ADMIN} specifically, not
 * {@code EDITOR} - a stricter rule than articles, which both roles can write. Several tests
 * here exist specifically to confirm that distinction is enforced, not assumed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCategoryControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ArticleRepository articleRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        categoryRepository.deleteAll();
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
    void createCategory_succeeds_withAdminToken() throws Exception {
        String token = generateToken("admin1", "ADMIN");

        Map<String, Object> request = Map.of(
                "name", "National News",
                "displayOrder", 10
        );

        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("National News")))
                .andExpect(jsonPath("$.data.slug", is("national-news")))
                .andExpect(jsonPath("$.data.active", is(true)));
    }

    @Test
    void createCategory_rejected_forEditorOnlyToken() throws Exception {
        // Category writes require ADMIN specifically - EDITOR alone (which CAN write articles)
        // must NOT be able to create categories. This is the key rule this test protects.
        String editorToken = generateToken("editor1", "EDITOR");

        Map<String, Object> request = Map.of("name", "Should Not Be Created");

        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategory_autoUniquifiesSlug_onDuplicateName() throws Exception {
        String token = generateToken("admin1", "ADMIN");
        Map<String, Object> request = Map.of("name", "Announcements");

        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug", is("announcements")));

        // Second category with the identical name must get a distinct, auto-suffixed slug
        // rather than failing or silently colliding.
        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug", is("announcements-2")));
    }

    @Test
    void updateCategory_succeeds_withAdminToken() throws Exception {
        Category existing = categoryRepository.save(Category.builder()
                .name("Old Name").slug("old-name").active(true).displayOrder(1).build());
        String token = generateToken("admin1", "ADMIN");

        Map<String, Object> updateRequest = Map.of(
                "name", "Updated Name",
                "displayOrder", 5,
                "active", true
        );

        mockMvc.perform(put("/api/admin/categories/{id}", existing.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Updated Name")))
                .andExpect(jsonPath("$.data.displayOrder", is(5)));
    }

    @Test
    void deleteCategory_succeeds_whenNoChildren() throws Exception {
        Category leaf = categoryRepository.save(Category.builder()
                .name("Leaf Category").slug("leaf-category").active(true).displayOrder(1).build());
        String token = generateToken("admin1", "ADMIN");

        mockMvc.perform(delete("/api/admin/categories/{id}", leaf.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void deleteCategory_rejected_whenHasChildCategories() throws Exception {
        Category parent = categoryRepository.save(Category.builder()
                .name("Parent").slug("parent").active(true).displayOrder(1).build());
        categoryRepository.save(Category.builder()
                .name("Child").slug("child").parent(parent).active(true).displayOrder(1).build());
        String token = generateToken("admin1", "ADMIN");

        // A parent with children must not be deletable - would orphan the child category.
        mockMvc.perform(delete("/api/admin/categories/{id}", parent.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void listCategories_accessible_toEditorRole() throws Exception {
        // Unlike write operations, read access to /api/admin/categories is allowed for
        // both ADMIN and EDITOR - editors need to see categories to assign articles to them.
        categoryRepository.save(Category.builder()
                .name("Visible Category").slug("visible-category").active(true).displayOrder(1).build());
        String editorToken = generateToken("editor1", "EDITOR");

        mockMvc.perform(get("/api/admin/categories")
                        .header("Authorization", "Bearer " + editorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void createCategory_rejected_withoutToken() throws Exception {
        Map<String, Object> request = Map.of("name", "Should Not Be Created");

        mockMvc.perform(post("/api/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}