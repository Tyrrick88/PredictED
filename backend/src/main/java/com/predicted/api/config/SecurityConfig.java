package com.predicted.api.config;

import com.predicted.api.auth.JwtAuthenticationFilter;
import com.predicted.api.persistence.AppUserRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RateLimitFilter rateLimitFilter,
      @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled
  )
      throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .headers(headers -> {
          String contentSecurityPolicy = h2ConsoleEnabled
              ? "default-src 'self' 'unsafe-inline' data:; frame-ancestors 'self'; base-uri 'none'; form-action 'self'"
              : "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'";
          headers
              .contentSecurityPolicy(csp -> csp.policyDirectives(
                  contentSecurityPolicy
              ))
              .contentTypeOptions(Customizer.withDefaults())
              .httpStrictTransportSecurity(hsts -> hsts
                  .includeSubDomains(true)
                  .preload(true)
                  .maxAgeInSeconds(31_536_000)
              )
              .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
              .permissionsPolicyHeader(permissions -> permissions.policy(
                  "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()"
              ));
          if (h2ConsoleEnabled) {
            headers.frameOptions(frameOptions -> frameOptions.sameOrigin());
          } else {
            headers.frameOptions(frameOptions -> frameOptions.deny());
          }
        })
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> {
          auth.requestMatchers(
                "/api/auth/login",
                "/api/auth/register",
                "/api/health",
                "/actuator/health"
            ).permitAll();
          if (h2ConsoleEnabled) {
            auth.requestMatchers("/h2-console/**").permitAll();
          }
          auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
              .anyRequest().authenticated();
        })
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
    FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
    FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  UserDetailsService userDetailsService(AppUserRepository userRepository) {
    return email -> userRepository.findByEmailIgnoreCase(email)
        .map(user -> User.withUsername(user.getEmail())
            .password(user.getPasswordHash())
            .roles(user.getRole().name())
            .build())
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${predicted.cors.allowed-origin-patterns:*}") String allowedOriginPatterns
  ) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(csv(allowedOriginPatterns));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
    configuration.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_DISPOSITION));
    configuration.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private List<String> csv(String value) {
    List<String> values = Arrays.stream(value.split(","))
        .map(String::trim)
        .map(this::normalizeOriginPattern)
        .filter(item -> !item.isBlank())
        .toList();
    return values.isEmpty() ? List.of("*") : values;
  }

  private String normalizeOriginPattern(String value) {
    String normalized = value.replaceFirst("^(https?://)(https?://)+", "$1");
    while (normalized.endsWith("/") && normalized.length() > 1) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}
