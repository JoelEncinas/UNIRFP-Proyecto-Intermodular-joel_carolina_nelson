package com.unir.bikeshare.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import com.unir.bikeshare.backend.users.model.User;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

  private static final String SECRET_KEY = "bikesharebikesharebikesharebikesharebikeshare123456";
  private static final String UID_CLAIM = "uid";

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
  }

  public String generateToken(User user) {
    return Jwts.builder()
        .subject(user.getUsername())
        .claim(UID_CLAIM, user.getId())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
        .signWith(getSigningKey())
        .compact();
  }

  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  public Long extractUserId(String token) {
    Object value = extractAllClaims(token).get(UID_CLAIM);
    if (value instanceof Integer intValue) {
      return intValue.longValue();
    }
    if (value instanceof Long longValue) {
      return longValue;
    }
    return null;
  }

  public boolean isTokenValid(String token) {
    Claims claims = extractAllClaims(token);
    Long userId = extractUserId(token);

    return userId != null && !isTokenExpired(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private boolean isTokenExpired(Claims claims) {
    Date expiration = claims.getExpiration();

    return expiration.before(new Date());
  }
}
