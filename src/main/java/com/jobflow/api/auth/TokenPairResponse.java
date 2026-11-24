package com.jobflow.api.auth;

public record TokenPairResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
