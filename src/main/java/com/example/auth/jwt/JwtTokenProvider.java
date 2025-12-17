package com.example.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final Key signingKey;
    private final long accessTokenValiditySeconds;

    public JwtTokenProvider(
        @Value("${auth.jwt.secret}") String secret,
        @Value("${auth.jwt.access-validity-seconds:3600}") long accessTokenValiditySeconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public String createAccessToken(JwtUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenValiditySeconds);
        var builder = Jwts.builder()
            .setSubject(principal.providerId())
            .claim("name", principal.name())
            .claim("nationality", principal.nationality())
            .claim("providerId", principal.providerId())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .signWith(signingKey, SignatureAlgorithm.HS256);

        if (principal.userId() != null) {
            builder.setId(principal.userId().toString());
        }
        return builder.compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public JwtUserPrincipal getPrincipal(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();

        Long userId = claims.getId() == null ? null : Long.parseLong(claims.getId());
        String name = claims.get("name", String.class);
        String nationality = claims.get("nationality", String.class);
        String providerId = claims.get("providerId", String.class);

        return new JwtUserPrincipal(userId, name, nationality, providerId);
    }
}
