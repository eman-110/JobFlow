package com.jobflow.api;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        int status,
        String message,
        Instant timestamp,
        Map<String, String> errors
) {
}
