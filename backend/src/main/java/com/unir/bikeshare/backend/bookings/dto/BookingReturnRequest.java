package com.unir.bikeshare.backend.bookings.dto;

import jakarta.validation.constraints.NotNull;

public record BookingReturnRequest(
        @NotNull Long stationId
) {}
