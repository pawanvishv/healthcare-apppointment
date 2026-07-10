package com.healthapp.backend.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.backend.application.port.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json);
            log.debug("Published event to topic {}: key={}", topic, key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish event to Kafka", e);
        }
    }
}
