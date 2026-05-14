package com.predicted.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class SecurityStartupValidator implements ApplicationRunner {

  private final Environment environment;
  private final String jwtSecret;
  private final String allowedOriginPatterns;
  private final boolean h2ConsoleEnabled;
  private final boolean demoUsersEnabled;
  private final boolean rateLimitEnabled;
  private final boolean productionMode;

  public SecurityStartupValidator(
      Environment environment,
      @Value("${predicted.security.jwt.secret}") String jwtSecret,
      @Value("${predicted.cors.allowed-origin-patterns:}") String allowedOriginPatterns,
      @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled,
      @Value("${predicted.seed.demo-users-enabled:true}") boolean demoUsersEnabled,
      @Value("${predicted.security.rate-limit.enabled:true}") boolean rateLimitEnabled,
      @Value("${predicted.security.production:false}") boolean productionMode
  ) {
    this.environment = environment;
    this.jwtSecret = jwtSecret;
    this.allowedOriginPatterns = allowedOriginPatterns;
    this.h2ConsoleEnabled = h2ConsoleEnabled;
    this.demoUsersEnabled = demoUsersEnabled;
    this.rateLimitEnabled = rateLimitEnabled;
    this.productionMode = productionMode;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!isProductionLike()) {
      return;
    }
    require(jwtSecret != null && jwtSecret.length() >= 64, "JWT_SECRET must be at least 64 characters.");
    String lowerSecret = jwtSecret == null ? "" : jwtSecret.toLowerCase();
    require(!lowerSecret.contains("replace-with") && !lowerSecret.contains("dev-only"),
        "JWT_SECRET must not use a placeholder value.");
    require(allowedOriginPatterns != null && !allowedOriginPatterns.isBlank(),
        "CORS_ALLOWED_ORIGIN_PATTERNS must name trusted frontend origins.");
    require(Arrays.stream(allowedOriginPatterns.split(",")).map(String::trim).noneMatch("*"::equals),
        "CORS_ALLOWED_ORIGIN_PATTERNS must not include '*'.");
    require(!h2ConsoleEnabled, "spring.h2.console.enabled must be false outside local development.");
    require(!demoUsersEnabled, "predicted.seed.demo-users-enabled must be false outside local development.");
    require(rateLimitEnabled, "predicted.security.rate-limit.enabled must remain true outside local development.");
  }

  private boolean isProductionLike() {
    return productionMode || Arrays.asList(environment.getActiveProfiles()).contains("postgres");
  }

  private void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException("Security startup validation failed: " + message);
    }
  }
}
