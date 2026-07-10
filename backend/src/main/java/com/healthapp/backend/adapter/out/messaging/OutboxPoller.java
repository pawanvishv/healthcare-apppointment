package com.healthapp.backend.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.backend.application.port.out.EventPublisherPort;
import com.healthapp.backend.application.port.out.OutboxRepositoryPort;
import com.healthapp.backend.domain.model.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepositoryPort outboxRepository;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    public void pollAndPublish() {
        var pendingEvents = outboxRepository.findPendingEvents(50);

        for (OutboxEvent event : pendingEvents) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(), Map.class);
                eventPublisher.publish(event.getEventType(), event.getAggregateId(), payload);
                outboxRepository.markAsSent(event.getId());
                log.debug("Outbox event sent: id={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.warn("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
