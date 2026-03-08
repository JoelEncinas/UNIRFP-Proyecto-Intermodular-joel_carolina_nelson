package com.unir.bikeshare.backend.users.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unir.bikeshare.backend.users.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

}
