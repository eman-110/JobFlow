package com.jobflow.model;

public enum JobStatus {
    SAVED,
    APPLIED,
    REJECTED,
    INTERVIEW;

    public boolean canTransitionTo(JobStatus nextStatus) {
        return switch (this) {
            case SAVED -> nextStatus == APPLIED;
            case APPLIED -> nextStatus == INTERVIEW || nextStatus == REJECTED;
            case INTERVIEW -> nextStatus == REJECTED;
            case REJECTED -> false;
        };
    }
}
