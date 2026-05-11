package com.predicted.api.auth;

import com.predicted.api.common.Models.UserProfile;

import java.time.Instant;

public record AuthResponse(
    String token,
    String tokenType,
    Instant expiresAt,
    UserProfile user
) {
}
