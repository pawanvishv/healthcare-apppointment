package com.healthapp.backend.adapter.out.persistence.repository;

import com.healthapp.backend.adapter.out.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    Optional<IdempotencyKeyEntity> findByIdempotencyKeyAndExpiresAtAfter(String idempotencyKey, Instant now);
}
