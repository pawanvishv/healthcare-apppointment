package com.healthapp.backend.application.port.out;

import java.util.Optional;

public interface IdempotencyStorePort {

    Optional<StoredResponse> findByKey(String key);

    void store(String key, String responseBody, int statusCode, java.time.Instant expiresAt);

    record StoredResponse(String responseBody, int statusCode) {}
}
