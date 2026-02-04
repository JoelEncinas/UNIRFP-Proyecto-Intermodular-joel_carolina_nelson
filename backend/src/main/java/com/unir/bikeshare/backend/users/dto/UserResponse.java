package com.unir.bikeshare.backend.users.dto;

import java.time.Instant;

import com.unir.bikeshare.backend.users.model.UserRole;

public record UserResponse(
		Long id,
        String username,
        String email,
        UserRole role,
        Instant createdAt
        ) {}
