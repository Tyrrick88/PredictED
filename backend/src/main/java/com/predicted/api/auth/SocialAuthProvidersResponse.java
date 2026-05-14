package com.predicted.api.auth;

public record SocialAuthProvidersResponse(
    boolean google,
    boolean apple
) {
}
