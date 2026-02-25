package com.unir.bikeshare.backend.bookings.dto;

import java.time.Instant;

import com.unir.bikeshare.backend.bookings.model.BookingStatus;

public record BookingUpdateRequest(
		BookingStatus status,
        Instant expiryTime
		) {}
