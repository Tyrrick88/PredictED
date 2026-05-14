package com.predicted.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final ObjectMapper objectMapper;
  private final boolean enabled;
  private final int authRequestsPerMinute;
  private final int uploadRequestsPerMinute;
  private final int aiRequestsPerMinute;
  private final int apiRequestsPerMinute;
  private final Clock clock;
  private final ConcurrentHashMap<String, FixedWindow> windows = new ConcurrentHashMap<>();

  @Autowired
  public RateLimitFilter(
      ObjectMapper objectMapper,
      @Value("${predicted.security.rate-limit.enabled:true}") boolean enabled,
      @Value("${predicted.security.rate-limit.auth-per-minute:8}") int authRequestsPerMinute,
      @Value("${predicted.security.rate-limit.uploads-per-minute:12}") int uploadRequestsPerMinute,
      @Value("${predicted.security.rate-limit.ai-per-minute:30}") int aiRequestsPerMinute,
      @Value("${predicted.security.rate-limit.api-per-minute:240}") int apiRequestsPerMinute
  ) {
    this(objectMapper, enabled, authRequestsPerMinute, uploadRequestsPerMinute, aiRequestsPerMinute,
        apiRequestsPerMinute, Clock.systemUTC());
  }

  RateLimitFilter(
      ObjectMapper objectMapper,
      boolean enabled,
      int authRequestsPerMinute,
      int uploadRequestsPerMinute,
      int aiRequestsPerMinute,
      int apiRequestsPerMinute,
      Clock clock
  ) {
    this.objectMapper = objectMapper;
    this.enabled = enabled;
    this.authRequestsPerMinute = authRequestsPerMinute;
    this.uploadRequestsPerMinute = uploadRequestsPerMinute;
    this.aiRequestsPerMinute = aiRequestsPerMinute;
    this.apiRequestsPerMinute = apiRequestsPerMinute;
    this.clock = clock;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !enabled
        || HttpMethod.OPTIONS.matches(request.getMethod())
        || path.equals("/api/health")
        || path.equals("/actuator/health")
        || !path.startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    Limit limit = limitFor(request);
    String key = limit.name() + ":" + clientKey(request);
    long now = clock.millis();
    FixedWindow window = windows.compute(key, (ignored, existing) -> {
      if (existing == null || now >= existing.resetAtMillis()) {
        return new FixedWindow(now + 60_000L, 1);
      }
      return existing.increment();
    });

    if (window.count() > limit.requestsPerMinute()) {
      long retryAfterSeconds = Math.max(1L, (window.resetAtMillis() - now + 999L) / 1000L);
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
      response.setHeader("X-RateLimit-Limit", String.valueOf(limit.requestsPerMinute()));
      response.setHeader("X-RateLimit-Remaining", "0");
      objectMapper.writeValue(response.getWriter(), Map.of(
          "code", "RATE_LIMITED",
          "message", "Too many requests. Please wait before trying again.",
          "timestamp", Instant.now(clock).toString()
      ));
      return;
    }

    response.setHeader("X-RateLimit-Limit", String.valueOf(limit.requestsPerMinute()));
    response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit.requestsPerMinute() - window.count())));
    filterChain.doFilter(request, response);
  }

  private Limit limitFor(HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    if (path.equals("/api/auth/login") || path.equals("/api/auth/register") || path.equals("/api/auth/social")) {
      return new Limit("auth", authRequestsPerMinute);
    }
    if (HttpMethod.POST.matches(method) && (path.equals("/api/marketplace/notes") || path.contains("/resources"))) {
      return new Limit("upload", uploadRequestsPerMinute);
    }
    if (path.startsWith("/api/tutor") || path.startsWith("/api/ai") || path.contains("/mock")
        || path.contains("/simulate")) {
      return new Limit("ai", aiRequestsPerMinute);
    }
    return new Limit("api", apiRequestsPerMinute);
  }

  private String clientKey(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private record Limit(String name, int requestsPerMinute) {
  }

  private record FixedWindow(long resetAtMillis, int count) {
    FixedWindow increment() {
      return new FixedWindow(resetAtMillis, count + 1);
    }
  }
}
