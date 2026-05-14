package com.predicted.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SocialAuthRequest(
    @NotBlank
    @Pattern(regexp = "(?i)google|apple", message = "Provider must be google or apple.")
    String provider,
    @NotBlank @Size(max = 8192) String idToken,
    @Size(max = 120) String name
) {
}
