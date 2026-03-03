package com.unir.bikeshare.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

  private static final String SECRET_KEY = "bikesharebikesharebikesharebikesharebikeshare123456";

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
  }

  public String generateToken(String username) {
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
        .signWith(getSigningKey())
        .compact();
  }

  public String extractUsername(String token) {
    Claims claims = Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();

    return claims.getSubject();
  }

  public boolean isTokenValid(String token, String username) {
    return extractUsername(token).equals(username);
  }
}