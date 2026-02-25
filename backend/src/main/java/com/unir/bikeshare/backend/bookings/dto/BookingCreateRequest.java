package com.unir.bikeshare.backend.bookings.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record BookingCreateRequest(
		@NotNull Long userId,
        @NotNull Long bikeId,
        Instant expiryTime
		) {}
