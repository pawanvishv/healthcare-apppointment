package com.healthapp.backend.config;

import com.healthapp.backend.adapter.out.persistence.entity.UserEntity;
import com.healthapp.backend.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataInitializer implements ApplicationRunner {

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        userRepository.findByEmail("doctor@healthapp.com").ifPresent(user -> {
            if (!passwordEncoder.matches("Password123!", user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode("Password123!"));
                userRepository.save(user);
                log.info("Updated seed doctor password");
            }
        });

        userRepository.findByEmail("patient@healthapp.com").ifPresent(user -> {
            if (!passwordEncoder.matches("Password123!", user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode("Password123!"));
                userRepository.save(user);
                log.info("Updated seed patient password");
            }
        });
    }
}
