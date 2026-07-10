package com.healthapp.backend.domain.exception;

public class IllegalStateTransitionException extends DomainException {

    public IllegalStateTransitionException(String message) {
        super("INVALID_STATE", message);
    }
}
