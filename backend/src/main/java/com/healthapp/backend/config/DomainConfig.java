package com.healthapp.backend.config;

import com.healthapp.backend.domain.service.CancellationPolicy;
import com.healthapp.backend.domain.service.SlotAvailabilityPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class DomainConfig {

    @Bean
    public SlotAvailabilityPolicy slotAvailabilityPolicy() {
        return new SlotAvailabilityPolicy();
    }

    @Bean
    public CancellationPolicy cancellationPolicy(
            @Value("${app.cancellation.window-hours:2}") long cancellationWindowHours) {
        return new CancellationPolicy(cancellationWindowHours);
    }
}
