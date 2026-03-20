package com.unir.bikeshare.backend.bookings.dto;

import java.time.Instant;

import com.unir.bikeshare.backend.bookings.model.BookingStatus;

public record BookingResponse(
		Long id,
        Long userId,
        String username,
        Long bikeId,
        String bikeModel,
        Long pickupStationId,
        String pickupStationName,
        Long dropoffStationId,
        String dropoffStationName,
        Instant startTime,
        Instant expiryTime,
        Instant activatedAt,
        Instant returnedAt,
        BookingStatus status
		) {}
