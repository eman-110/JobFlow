package com.jobflow.api;

public record JobResponse(
        int id,
        String title,
        String company,
        double salary
) {
}
