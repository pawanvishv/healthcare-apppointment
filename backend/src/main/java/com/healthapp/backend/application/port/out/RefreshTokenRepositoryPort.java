package com.healthapp.backend.application.port.out;

import java.util.Optional;

public interface RefreshTokenRepositoryPort {

    void save(String userId, String tokenHash, java.time.Instant expiresAt);

    Optional<StoredRefreshToken> findByTokenHash(String tokenHash);

    void revoke(String tokenId);

    void revokeAllForUser(String userId);

    record StoredRefreshToken(String id, String userId, String tokenHash,
                              java.time.Instant expiresAt, boolean revoked) {}
}
