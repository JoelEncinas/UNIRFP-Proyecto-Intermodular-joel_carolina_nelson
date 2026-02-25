package com.unir.bikeshare.backend.bookings.dto;

import java.time.Instant;

import com.unir.bikeshare.backend.bookings.model.BookingStatus;

public record BookingResponse(
		Long id,
        Long userId,
        String username,
        Long bikeId,
        String bikeModel,
        Instant startTime,
        Instant expiryTime,
        BookingStatus status
		) {}
