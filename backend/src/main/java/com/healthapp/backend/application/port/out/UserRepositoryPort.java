package com.healthapp.backend.application.port.out;

import com.healthapp.backend.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(String id);

    boolean existsByEmail(String email);
}
