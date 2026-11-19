package com.jobflow.exception;

public class JobCapacityExceededException extends RuntimeException {
    public JobCapacityExceededException(String message) {
        super(message);
    }
}
