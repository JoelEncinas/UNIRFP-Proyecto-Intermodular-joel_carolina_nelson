package com.unir.bikeshare.backend.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
		@NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 6, max = 255) String password
		) {}
