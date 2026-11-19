package com.jobflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class JobExceptionHandler {

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleJobNotFound(JobNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(JobCapacityExceededException.class)
    public ResponseEntity<ProblemDetail> handleCapacityExceeded(JobCapacityExceededException exception) {
        return problem(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRequest(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, Exception exception) {
        return ResponseEntity.status(status)
                .body(ProblemDetail.forStatusAndDetail(status, exception.getMessage()));
    }
}
