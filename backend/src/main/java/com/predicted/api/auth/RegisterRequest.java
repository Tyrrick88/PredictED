package com.predicted.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegisterRequest(
    @NotBlank @Size(max = 120) String name,
    @Email @NotBlank @Size(max = 180) String email,
    @NotBlank
    @Size(min = 10, max = 72)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must include letters and numbers")
    String password,
    @NotBlank @Size(max = 140) String university,
    @NotBlank @Size(max = 140) String program,
    @NotBlank @Size(max = 80) String academicLevel,
    @Size(min = 1) List<@NotBlank String> courseIds
) {

  public RegisterRequest(
      String name,
      String email,
      String password,
      String university,
      String program,
      String academicLevel
  ) {
    this(name, email, password, university, program, academicLevel, null);
  }
}
