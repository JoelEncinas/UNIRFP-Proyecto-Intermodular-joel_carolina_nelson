package com.unir.bikeshare.backend.bikes.dto;

import com.unir.bikeshare.backend.bikes.model.BikeStatus;

public record BikeResponse(
		Long id,
        String model,
        BikeStatus status,
        Long stationId
		) {}
