package com.predicted.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.predicted.api.common.BadRequestException;
import com.predicted.api.persistence.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class SocialAuthService {

  private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
  private static final Set<String> APPLE_ISSUERS = Set.of("https://appleid.apple.com");

  private final OidcIdTokenVerifier tokenVerifier;
  private final UserRegistrationService userRegistrationService;
  private final String googleClientId;
  private final String appleClientId;
  private final String googleJwksUri;
  private final String appleJwksUri;

  public SocialAuthService(
      OidcIdTokenVerifier tokenVerifier,
      UserRegistrationService userRegistrationService,
      @Value("${predicted.oauth.google.client-id:}") String googleClientId,
      @Value("${predicted.oauth.apple.client-id:}") String appleClientId,
      @Value("${predicted.oauth.google.jwks-uri:https://www.googleapis.com/oauth2/v3/certs}") String googleJwksUri,
      @Value("${predicted.oauth.apple.jwks-uri:https://appleid.apple.com/auth/keys}") String appleJwksUri
  ) {
    this.tokenVerifier = tokenVerifier;
    this.userRegistrationService = userRegistrationService;
    this.googleClientId = googleClientId;
    this.appleClientId = appleClientId;
    this.googleJwksUri = googleJwksUri;
    this.appleJwksUri = appleJwksUri;
  }

  public SocialAuthProvidersResponse providers() {
    return new SocialAuthProvidersResponse(configured(googleClientId), configured(appleClientId));
  }

  public AppUser authenticate(SocialAuthRequest request) {
    VerifiedSocialProfile profile = switch (request.provider().trim().toLowerCase(Locale.ROOT)) {
      case "google" -> verifyProvider(
          "Google",
          "google",
          request.idToken(),
          request.name(),
          googleClientId,
          googleJwksUri,
          GOOGLE_ISSUERS
      );
      case "apple" -> verifyProvider(
          "Apple",
          "apple",
          request.idToken(),
          request.name(),
          appleClientId,
          appleJwksUri,
          APPLE_ISSUERS
      );
      default -> throw new BadRequestException("Provider must be google or apple.");
    };

    return userRegistrationService.findOrCreateSocialUser(profile.email(), profile.name());
  }

  private VerifiedSocialProfile verifyProvider(
      String label,
      String provider,
      String idToken,
      String requestName,
      String clientId,
      String jwksUri,
      Set<String> issuers
  ) {
    if (!configured(clientId)) {
      throw new BadRequestException(label + " sign-in is not configured.");
    }
    JsonNode claims = tokenVerifier.verify(idToken, jwksUri, clientId.trim(), issuers);
    String subject = claims.path("sub").asText("");
    String email = normalizeEmail(claims.path("email").asText(""));
    if (subject.isBlank() || email.isBlank()) {
      throw new BadRequestException("Social sign-in token is missing account details.");
    }
    if (!emailVerified(claims)) {
      throw new BadRequestException("Social sign-in requires a verified email address.");
    }
    return new VerifiedSocialProfile(
        provider,
        subject,
        email,
        resolveName(claims.path("name").asText(""), requestName, email)
    );
  }

  private String normalizeEmail(String value) {
    String email = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    if (email.length() > 180 || !email.contains("@")) {
      throw new BadRequestException("Social sign-in token is missing account details.");
    }
    return email;
  }

  private boolean emailVerified(JsonNode claims) {
    JsonNode value = claims.get("email_verified");
    if (value == null || value.isNull()) {
      return false;
    }
    if (value.isBoolean()) {
      return value.booleanValue();
    }
    return "true".equalsIgnoreCase(value.asText());
  }

  private String resolveName(String claimName, String requestName, String email) {
    for (String candidate : new String[] { claimName, requestName, emailLocalPart(email), "PredictED Student" }) {
      String clean = clean(candidate, 120);
      if (!clean.isBlank()) {
        return clean;
      }
    }
    return "PredictED Student";
  }

  private String emailLocalPart(String email) {
    int at = email == null ? -1 : email.indexOf('@');
    if (at <= 0) {
      return "";
    }
    return email.substring(0, at).replace('.', ' ');
  }

  private String clean(String value, int maxLength) {
    String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
    return clean.length() <= maxLength ? clean : clean.substring(0, maxLength).trim();
  }

  private boolean configured(String value) {
    return value != null && !value.isBlank();
  }
}
