package com.healthapp.backend.adapter.out.persistence.repository;

import com.healthapp.backend.adapter.out.persistence.entity.OutboxEventEntity;
import com.healthapp.backend.domain.model.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, String> {

    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status,
                                                             org.springframework.data.domain.Pageable pageable);
}
