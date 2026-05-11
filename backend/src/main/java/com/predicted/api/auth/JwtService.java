package com.predicted.api.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

  private final SecretKey secretKey;
  private final String issuer;
  private final long expirationMinutes;

  public JwtService(
      @Value("${predicted.security.jwt.secret}") String secret,
      @Value("${predicted.security.jwt.issuer}") String issuer,
      @Value("${predicted.security.jwt.expiration-minutes}") long expirationMinutes
  ) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.issuer = issuer;
    this.expirationMinutes = expirationMinutes;
  }

  public IssuedToken generateToken(UserDetails userDetails) {
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plus(expirationMinutes, ChronoUnit.MINUTES);
    List<String> roles = userDetails.getAuthorities()
        .stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

    String value = Jwts.builder()
        .issuer(issuer)
        .subject(userDetails.getUsername())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .claim("roles", roles)
        .signWith(secretKey)
        .compact();

    return new IssuedToken(value, expiresAt);
  }

  public String extractUsername(String token) {
    return parseClaims(token).getSubject();
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    Claims claims = parseClaims(token);
    return userDetails.getUsername().equals(claims.getSubject())
        && claims.getExpiration().toInstant().isAfter(Instant.now());
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public record IssuedToken(String value, Instant expiresAt) {
  }
}
