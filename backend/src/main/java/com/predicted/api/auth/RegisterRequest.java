package com.predicted.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 120) String name,
    @Email @NotBlank @Size(max = 180) String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size(max = 140) String university,
    @NotBlank @Size(max = 140) String program,
    @NotBlank @Size(max = 80) String academicLevel
) {
}
