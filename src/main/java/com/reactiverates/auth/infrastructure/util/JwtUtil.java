package com.reactiverates.auth.infrastructure.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    @Value("${jwt.secret:mySecretKeymySecretKeymySecretKeymySecretKey}")
    private String secret;

    @Value("${jwt.access-token.expiration:900000}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:604800000}")
    private Long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public String extractTokenId(String token) {
        return extractClaim(token, claims -> claims.get("tokenId", String.class));
    }
    
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> extractAuthorities(String token) {
        return extractClaim(token, claims -> {
            Object authoritiesObj = claims.get("authorities");
            if (authoritiesObj instanceof List) {
                return (List<Map<String, String>>) authoritiesObj;
            }
            return List.of();
        });
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private boolean isTokenExpired(String token) { 
        return extractExpiration(token).before(new Date());
    }

    public String generateAccessToken(UserDetails userDetails) { 
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("authorities", userDetails.getAuthorities().stream()
            .map(authority -> Map.of("authority", authority.getAuthority()))
            .toList());
        return createToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) { 
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("authorities", userDetails.getAuthorities().stream()
            .map(authority -> Map.of("authority", authority.getAuthority()))
            .toList());
        return createToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }
    
    public String generateRefreshToken(UserDetails userDetails, String tokenId) { 
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("tokenId", tokenId);
        claims.put("authorities", userDetails.getAuthorities().stream()
            .map(authority -> Map.of("authority", authority.getAuthority()))
            .toList());
        return createToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String username, Long expiration) {
        return Jwts.builder()
            .claims(claims)
            .subject(username)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean isAccessToken(String token) {
        try {
            String tokenType = extractClaim(token, claims -> claims.get("type", String.class));
            return "access".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            String tokenType = extractClaim(token, claims -> claims.get("type", String.class));
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }
}