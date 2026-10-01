package bf.formation.assoue.auth.service;

import bf.formation.assoue.common.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

/** Test unitaire pur (pas de contexte Spring) : verifie le contenu du JWT genere. */
class JwtServiceTest {

    private static final String SECRET = "secret_de_test_assoue_store_2026_minimum_32_caracteres";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    @Test
    void generateToken_shouldContainUserIdAndRoleClaims() {
        String token = jwtService.generateToken(42L, "70000000", "CITOYEN");

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("70000000");
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(42L);
        assertThat(claims.get("role", String.class)).isEqualTo("CITOYEN");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }
}
