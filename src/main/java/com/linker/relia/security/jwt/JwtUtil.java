package com.linker.relia.security.jwt;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Getter
@Component
public class JwtUtil {
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = new SecretKeySpec(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );
    }

    public String getIssuer() {
        return jwtProperties.getIssuer();
    }

    public long getAccessExpiredMs() {
        return jwtProperties.getAccessExpiredMs();
    }

    public long getRefreshExpiredMs() {
        return jwtProperties.getRefreshExpiredMs();
    }

    public String getTokenId(String token) {
        return getClaims(token).getId();
    }

    public String getCategory(String token) {
        return getClaims(token).get("category", String.class);
    }

    public String getLoginId(String token) {
        return getClaims(token).get("loginId", String.class);
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public long getRemainingExpirationMillis(String token) {
        Date expiration = getClaims(token).getExpiration();
        return Math.max(expiration.getTime() - System.currentTimeMillis(), 0);
    }

    public boolean isExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    public String createAccessToken(String username, String role) {
        return createJwt("AccessToken", username, role);
    }

    public String createRefreshToken(String username, String role) {
        return createJwt("RefreshToken", username, role);
    }

    private String createJwt(String category, String loginId, String role) {
        long expiredMs = resolveExpiredMs(category);

        return Jwts.builder()
                .header().add("typ", "JWT").and()
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.getIssuer())
                .claim("category", category)
                .claim("loginId", loginId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            return exception.getClaims();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(AuthErrorCode.USER_UNAUTHORIZED);
        }
    }

    private long resolveExpiredMs(String category) {
        return switch (category) {
            case "AccessToken" -> jwtProperties.getAccessExpiredMs();
            case "RefreshToken" -> jwtProperties.getRefreshExpiredMs();
            default -> throw new BusinessException(AuthErrorCode.USER_UNAUTHORIZED);
        };
    }
}
