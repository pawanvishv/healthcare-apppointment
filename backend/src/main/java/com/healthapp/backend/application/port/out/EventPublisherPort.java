package com.healthapp.backend.application.port.out;

public interface EventPublisherPort {

    void publish(String topic, String key, Object payload);
}
