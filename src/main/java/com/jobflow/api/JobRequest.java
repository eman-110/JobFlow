package com.jobflow.api;

public record JobRequest(
        String title,
        String company,
        String location,
        double salary
) {
}
