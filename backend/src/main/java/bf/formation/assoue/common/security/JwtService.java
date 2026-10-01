package bf.formation.assoue.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Emet les JWT. Le meme secret (jwt.secret) doit etre configure dans TOUS
 * les microservices pour qu'ils puissent valider les tokens emis ici,
 * sans devoir interroger service-auth a chaque requete (pattern stateless).
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(Long userId, String telephone, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(telephone)
                .claims(Map.of("userId", userId, "role", role))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }
}
