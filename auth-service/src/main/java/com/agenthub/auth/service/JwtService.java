package com.agenthub.auth.service;

import com.agenthub.auth.entity.AuthUser;
import com.agenthub.auth.security.JwtKeyProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final JwtKeyProvider jwtKeyProvider;

    @Value("${jwt.expiration-seconds:3600}")
    private long expirationSeconds;

    @Value("${jwt.issuer:auth-service}")
    private String issuer;

    public JwtService(JwtKeyProvider jwtKeyProvider) {
        this.jwtKeyProvider = jwtKeyProvider;
    }

    public String generateToken(AuthUser authUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("customerId", authUser.getCustomerId());
        claims.put("userId", authUser.getId());
        claims.put("email", authUser.getEmail());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (expirationSeconds * 1000));

        return Jwts.builder()
                .claims(claims)
                .subject(authUser.getUsername())
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtKeyProvider.getPrivateKey())
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtKeyProvider.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return validateToken(token).getSubject();
    }

    public Long getCustomerIdFromToken(String token) {
        return validateToken(token).get("customerId", Long.class);
    }

    public Long getUserIdFromToken(String token) {
        return validateToken(token).get("userId", Long.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = validateToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
