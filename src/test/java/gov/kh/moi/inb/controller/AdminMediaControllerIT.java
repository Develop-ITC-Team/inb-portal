package gov.kh.moi.inb.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the admin media upload endpoint - the only piece of this service that
 * accepts arbitrary binary input from a client, which makes its validation the highest-risk
 * surface in the codebase. These tests specifically target {@link
 * gov.kh.moi.inb.service.impl.LocalFileStorageServiceImpl}'s three-layer check (size, claimed
 * extension, sniffed content) rather than just the happy path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminMediaControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Real PNG magic bytes - enough for Tika's magic-byte detector to identify it as image/png
    // without needing a fully valid PNG body.
    private static final byte[] PNG_MAGIC_BYTES =
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

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
    void upload_succeeds_withValidPngAndAdminToken() throws Exception {
        String token = generateToken("admin1", "ADMIN");
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_MAGIC_BYTES);

        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fileUrl", endsWith(".png")));
    }

    @Test
    void upload_succeeds_withEditorToken() throws Exception {
        // Media upload allows both ADMIN and EDITOR, same as article writes.
        String token = generateToken("editor1", "EDITOR");
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_MAGIC_BYTES);

        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    @Test
    void upload_rejected_withoutToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_MAGIC_BYTES);

        mockMvc.perform(multipart("/api/admin/media/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_rejected_forRoleOutsideAdminOrEditor() throws Exception {
        String token = generateToken("viewer1", "VIEWER");
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_MAGIC_BYTES);

        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_rejected_whenFileIsEmpty() throws Exception {
        String token = generateToken("admin1", "ADMIN");
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_rejected_whenExtensionNotInAllowList() throws Exception {
        String token = generateToken("admin1", "ADMIN");
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "irrelevant content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_rejected_whenContentDoesNotMatchClaimedExtension() throws Exception {
        // The crux of the security control: an allowed extension (.jpg) on a file whose actual
        // bytes are plain text, not an image, must still be rejected - detection is content-based
        // (Tika), not filename-based.
        String token = generateToken("admin1", "ADMIN");
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg",
                "This is plain text, not a real JPEG.".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void upload_rejected_whenFileExceedsMaxSize() throws Exception {
        String token = generateToken("admin1", "ADMIN");
        // One byte over the 10MB limit enforced explicitly in LocalFileStorageServiceImpl.
        // (MockMvc builds the multipart request directly, bypassing the servlet container's own
        // max-file-size parsing, so it's this application-level check that fires here.)
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);

        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Uploaded file exceeds the maximum allowed size of 10MB")));
    }
}
