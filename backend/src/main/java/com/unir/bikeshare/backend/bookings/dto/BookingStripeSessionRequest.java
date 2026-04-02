package com.unir.bikeshare.backend.bookings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingStripeSessionRequest(
        @NotNull Long bookingId,
        @NotBlank String sessionId
) {
}
