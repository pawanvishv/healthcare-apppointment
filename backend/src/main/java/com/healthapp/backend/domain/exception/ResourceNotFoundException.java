package com.healthapp.backend.domain.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, String id) {
        super("NOT_FOUND", resource + " not found: " + id);
    }
}
