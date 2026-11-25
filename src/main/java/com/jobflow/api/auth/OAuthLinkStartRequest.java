package com.jobflow.api.auth;

import jakarta.validation.constraints.NotBlank;

public record OAuthLinkStartRequest(@NotBlank String codeChallenge) {}
