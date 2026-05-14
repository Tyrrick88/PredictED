package com.predicted.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.predicted.api.common.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OidcIdTokenVerifier {

  private static final Duration JWKS_CACHE_TTL = Duration.ofHours(6);
  private static final long CLOCK_SKEW_SECONDS = 60;
  private static final String INVALID_TOKEN_MESSAGE = "Social sign-in token could not be verified.";

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final Clock clock;
  private final Duration requestTimeout;
  private final ConcurrentHashMap<String, CachedJwks> jwksCache = new ConcurrentHashMap<>();

  @Autowired
  public OidcIdTokenVerifier(
      ObjectMapper objectMapper,
      @Value("${predicted.oauth.http-timeout-seconds:5}") int timeoutSeconds
  ) {
    this(
        objectMapper,
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build(),
        Clock.systemUTC(),
        Duration.ofSeconds(timeoutSeconds)
    );
  }

  OidcIdTokenVerifier(
      ObjectMapper objectMapper,
      HttpClient httpClient,
      Clock clock,
      Duration requestTimeout
  ) {
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
    this.clock = clock;
    this.requestTimeout = requestTimeout;
  }

  public JsonNode verify(
      String idToken,
      String jwksUri,
      String expectedAudience,
      Set<String> allowedIssuers
  ) {
    String[] parts = idToken.trim().split("\\.", -1);
    if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
      throw invalidToken();
    }

    JsonNode header = decodeJsonPart(parts[0]);
    if (!"RS256".equals(header.path("alg").asText())) {
      throw invalidToken();
    }
    String keyId = header.path("kid").asText("");
    if (keyId.isBlank()) {
      throw invalidToken();
    }

    PublicKey publicKey = publicKeyFor(jwksUri, keyId);
    verifySignature(parts, publicKey);

    JsonNode claims = decodeJsonPart(parts[1]);
    validateClaims(claims, expectedAudience, allowedIssuers);
    return claims;
  }

  private JsonNode decodeJsonPart(String encoded) {
    try {
      return objectMapper.readTree(Base64.getUrlDecoder().decode(encoded));
    } catch (IllegalArgumentException | IOException exception) {
      throw invalidToken(exception);
    }
  }

  private PublicKey publicKeyFor(String jwksUri, String keyId) {
    JsonNode key = keysFor(jwksUri, false).get(keyId);
    if (key == null) {
      key = keysFor(jwksUri, true).get(keyId);
    }
    if (key == null || !"RSA".equals(key.path("kty").asText())) {
      throw invalidToken();
    }
    String modulus = key.path("n").asText("");
    String exponent = key.path("e").asText("");
    if (modulus.isBlank() || exponent.isBlank()) {
      throw invalidToken();
    }
    try {
      RSAPublicKeySpec spec = new RSAPublicKeySpec(unsignedInteger(modulus), unsignedInteger(exponent));
      return KeyFactory.getInstance("RSA").generatePublic(spec);
    } catch (IllegalArgumentException | GeneralSecurityException exception) {
      throw invalidToken(exception);
    }
  }

  private Map<String, JsonNode> keysFor(String jwksUri, boolean forceRefresh) {
    Instant now = Instant.now(clock);
    CachedJwks cached = jwksCache.get(jwksUri);
    if (!forceRefresh && cached != null && cached.expiresAt().isAfter(now)) {
      return cached.keys();
    }

    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(jwksUri))
          .GET()
          .timeout(requestTimeout)
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw invalidToken();
      }
      JsonNode jwks = objectMapper.readTree(response.body()).path("keys");
      if (!jwks.isArray()) {
        throw invalidToken();
      }
      Map<String, JsonNode> keys = new HashMap<>();
      jwks.forEach(key -> {
        String keyId = key.path("kid").asText("");
        if (!keyId.isBlank()) {
          keys.put(keyId, key);
        }
      });
      if (keys.isEmpty()) {
        throw invalidToken();
      }
      jwksCache.put(jwksUri, new CachedJwks(keys, now.plus(JWKS_CACHE_TTL)));
      return keys;
    } catch (IOException exception) {
      throw invalidToken(exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw invalidToken(exception);
    } catch (IllegalArgumentException exception) {
      throw invalidToken(exception);
    }
  }

  private void verifySignature(String[] parts, PublicKey publicKey) {
    try {
      Signature verifier = Signature.getInstance("SHA256withRSA");
      verifier.initVerify(publicKey);
      verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
      if (!verifier.verify(Base64.getUrlDecoder().decode(parts[2]))) {
        throw invalidToken();
      }
    } catch (IllegalArgumentException | GeneralSecurityException exception) {
      throw invalidToken(exception);
    }
  }

  private void validateClaims(JsonNode claims, String expectedAudience, Set<String> allowedIssuers) {
    String issuer = claims.path("iss").asText("");
    if (!allowedIssuers.contains(issuer)) {
      throw invalidToken();
    }
    if (!audienceMatches(claims.get("aud"), expectedAudience)) {
      throw invalidToken();
    }
    JsonNode authorizedParty = claims.get("azp");
    if (authorizedParty != null && !expectedAudience.equals(authorizedParty.asText())) {
      throw invalidToken();
    }

    long now = Instant.now(clock).getEpochSecond();
    long expiresAt = claims.path("exp").asLong(0);
    if (expiresAt == 0 || expiresAt + CLOCK_SKEW_SECONDS < now) {
      throw invalidToken();
    }
    JsonNode notBefore = claims.get("nbf");
    if (notBefore != null && notBefore.asLong(Long.MAX_VALUE) - CLOCK_SKEW_SECONDS > now) {
      throw invalidToken();
    }
    JsonNode issuedAt = claims.get("iat");
    if (issuedAt != null && issuedAt.asLong(0) - CLOCK_SKEW_SECONDS > now) {
      throw invalidToken();
    }
  }

  private boolean audienceMatches(JsonNode audience, String expectedAudience) {
    if (audience == null || expectedAudience == null || expectedAudience.isBlank()) {
      return false;
    }
    if (audience.isTextual()) {
      return expectedAudience.equals(audience.asText());
    }
    if (audience.isArray()) {
      for (JsonNode value : audience) {
        if (expectedAudience.equals(value.asText())) {
          return true;
        }
      }
    }
    return false;
  }

  private BigInteger unsignedInteger(String encoded) {
    return new BigInteger(1, Base64.getUrlDecoder().decode(encoded));
  }

  private BadRequestException invalidToken() {
    return new BadRequestException(INVALID_TOKEN_MESSAGE);
  }

  private BadRequestException invalidToken(Throwable cause) {
    return new BadRequestException(INVALID_TOKEN_MESSAGE, cause);
  }

  private record CachedJwks(Map<String, JsonNode> keys, Instant expiresAt) {
  }
}
