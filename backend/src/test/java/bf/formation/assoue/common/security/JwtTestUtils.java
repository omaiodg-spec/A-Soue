package bf.formation.assoue.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Genere des JWT signes avec le meme secret que application-test.properties,
 * pour simuler un utilisateur CITOYEN / ENTREPRISE / ADMIN dans les tests
 * d'integration sans devoir appeler service-auth.
 */
public final class JwtTestUtils {

    private static final String SECRET = "secret_de_test_assoue_store_2026_minimum_32_caracteres";

    private JwtTestUtils() {
    }

    public static String token(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Date now = new Date();
        return Jwts.builder()
                .subject("test-" + userId)
                .claims(Map.of("userId", userId, "role", role))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000))
                .signWith(key)
                .compact();
    }

    public static String expiredToken(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Date past = new Date(System.currentTimeMillis() - 3_600_000);
        return Jwts.builder()
                .subject("test-" + userId)
                .claims(Map.of("userId", userId, "role", role))
                .issuedAt(new Date(past.getTime() - 1000))
                .expiration(past)
                .signWith(key)
                .compact();
    }

    public static String tokenSigneAvecMauvaisSecret(Long userId, String role) {
        SecretKey wrongKey = Keys.hmacShaKeyFor("un_secret_totalement_different_qui_ne_doit_pas_marcher".getBytes());
        Date now = new Date();
        return Jwts.builder()
                .subject("test-" + userId)
                .claims(Map.of("userId", userId, "role", role))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000))
                .signWith(wrongKey)
                .compact();
    }
}
