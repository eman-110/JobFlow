package com.jobflow.api;

public record CsrfTokenResponse(String headerName, String token) {
}
