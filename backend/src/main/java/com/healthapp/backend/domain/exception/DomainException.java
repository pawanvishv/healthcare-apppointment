package com.healthapp.backend.domain.exception;

public class DomainException extends RuntimeException {

    private final String errorCode;

    public DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
