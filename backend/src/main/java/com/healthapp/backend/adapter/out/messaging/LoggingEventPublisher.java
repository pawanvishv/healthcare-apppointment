package com.healthapp.backend.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.backend.application.port.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class LoggingEventPublisher implements EventPublisherPort {

    private final ObjectMapper objectMapper;

    @Override
    public void publish(String topic, String key, Object payload) {
        try {
            log.info("Event published (dev mode, Kafka disabled): topic={}, key={}, payload={}",
                    topic, key, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("Failed to serialize event payload for logging", e);
        }
    }
}
