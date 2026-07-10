package com.healthapp.backend.application.port.out;

import com.healthapp.backend.domain.model.OutboxEvent;

import java.util.List;

public interface OutboxRepositoryPort {

    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findPendingEvents(int limit);

    void markAsSent(String eventId);
}
