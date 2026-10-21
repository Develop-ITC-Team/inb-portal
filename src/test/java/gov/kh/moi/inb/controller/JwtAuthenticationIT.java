package gov.kh.moi.inb.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end proof that {@link gov.kh.moi.inb.security.JwtAuthenticationFilter} +
 * {@link gov.kh.moi.inb.security.JwtTokenProvider} actually reject bad tokens on a real request,
 * not just in isolation - complementing JwtTokenProviderTest's unit-level checks and the
 * existing "no token at all" case already covered elsewhere.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationIT {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Test
    void adminEndpoint_rejected_withExpiredToken() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        String expired = Jwts.builder()
                .subject("admin1")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/admin/articles").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_rejected_withTokenSignedByWrongSecret() throws Exception {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "a_completely_different_secret_key_at_least_256_bits_long_0987654321".getBytes());
        String tampered = Jwts.builder()
                .subject("admin1")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(wrongKey)
                .compact();

        mockMvc.perform(get("/api/admin/articles").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_rejected_withMalformedAuthorizationHeader() throws Exception {
        // Missing the "Bearer " prefix entirely - the filter must not treat this as a token.
        mockMvc.perform(get("/api/admin/articles").header("Authorization", "garbage-not-a-bearer-token"))
                .andExpect(status().isUnauthorized());
    }
}
