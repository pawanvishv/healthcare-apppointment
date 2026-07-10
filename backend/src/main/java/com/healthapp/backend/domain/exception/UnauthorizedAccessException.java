package com.healthapp.backend.domain.exception;

public class UnauthorizedAccessException extends DomainException {

    public UnauthorizedAccessException() {
        super("FORBIDDEN", "Not authorized to access this resource");
    }
}
