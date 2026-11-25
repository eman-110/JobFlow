package com.jobflow.api.auth;

import jakarta.validation.constraints.NotBlank;

public record OAuthExchangeRequest(@NotBlank String code, @NotBlank String clientId,
                                   @NotBlank String codeVerifier) {}
