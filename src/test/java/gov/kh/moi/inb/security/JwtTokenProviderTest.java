package gov.kh.moi.inb.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtTokenProvider} - the sole gatekeeper deciding whether a bearer token
 * is trusted. Validity checks (expiry, signature, malformed input) matter more here than
 * anywhere else in the service, since every admin endpoint's auth depends on this class alone.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "unit_test_secret_key_must_be_at_least_256_bits_long_1234567890";

    private JwtTokenProvider provider;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() throws Exception {
        provider = new JwtTokenProvider(SECRET);
        // @PostConstruct isn't run outside a Spring context - invoke it directly.
        Method init = JwtTokenProvider.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(provider);

        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    private String validToken(String username, List<String> roles) {
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(signingKey)
                .compact();
    }

    @Test
    void isValid_returnsTrue_forWellFormedToken() {
        String token = validToken("admin1", List.of("ADMIN"));
        assertThat(provider.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalse_forExpiredToken() {
        String expired = Jwts.builder()
                .subject("admin1")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(signingKey)
                .compact();

        assertThat(provider.isValid(expired)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forTokenSignedWithWrongSecret() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "a_completely_different_secret_key_at_least_256_bits_long_0987654321".getBytes());
        String tampered = Jwts.builder()
                .subject("admin1")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(wrongKey)
                .compact();

        assertThat(provider.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forMalformedToken() {
        assertThat(provider.isValid("not.a.jwt")).isFalse();
    }

    @Test
    void isValid_returnsFalse_forBlankToken() {
        assertThat(provider.isValid("")).isFalse();
    }

    @Test
    void getUsername_returnsSubjectClaim() {
        String token = validToken("editor1", List.of("EDITOR"));
        assertThat(provider.getUsername(token)).isEqualTo("editor1");
    }

    @Test
    void getRoles_returnsRolesClaimAsStringList() {
        String token = validToken("admin1", List.of("ADMIN", "EDITOR"));
        assertThat(provider.getRoles(token)).containsExactly("ADMIN", "EDITOR");
    }

    @Test
    void getRoles_returnsEmptyList_whenRolesClaimAbsent() {
        String token = Jwts.builder()
                .subject("no-roles-user")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(signingKey)
                .compact();

        assertThat(provider.getRoles(token)).isEmpty();
    }
}
