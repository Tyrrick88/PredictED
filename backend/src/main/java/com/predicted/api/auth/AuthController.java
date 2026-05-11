package com.predicted.api.auth;

import com.predicted.api.common.Models.UserProfile;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody AuthRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password())
    );
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    JwtService.IssuedToken issuedToken = jwtService.generateToken(userDetails);
    return new AuthResponse(
        issuedToken.value(),
        "Bearer",
        issuedToken.expiresAt(),
        profileFor(userDetails.getUsername())
    );
  }

  @GetMapping("/me")
  public UserProfile me(Principal principal) {
    return profileFor(principal.getName());
  }

  public static UserProfile profileFor(String email) {
    if ("admin@predicted.test".equalsIgnoreCase(email)) {
      return new UserProfile(
          "usr_admin",
          "Grace Wanjiku",
          email,
          "predictED Operations",
          "Platform Administration",
          "Admin",
          "ADMIN"
      );
    }
    return new UserProfile(
        "usr_alex",
        "Alex Mwangi",
        email,
        "University of Nairobi",
        "BSc. Computer Science",
        "Year 3, Semester 2",
        "STUDENT"
    );
  }
}
