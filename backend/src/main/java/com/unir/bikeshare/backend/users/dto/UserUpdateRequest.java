package com.unir.bikeshare.backend.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @Size(max = 50) String username,

        @Email @Size(max = 100) String email,

        @Size(min = 6, max = 255) String password) {
}