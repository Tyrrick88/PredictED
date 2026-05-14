package com.predicted.api.auth;

record VerifiedSocialProfile(
    String provider,
    String subject,
    String email,
    String name
) {
}
