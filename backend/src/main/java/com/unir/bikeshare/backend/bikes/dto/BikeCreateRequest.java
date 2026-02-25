package com.unir.bikeshare.backend.bikes.dto;

import com.unir.bikeshare.backend.bikes.model.BikeStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BikeCreateRequest(
		@NotBlank @Size(max = 50) String model,
        BikeStatus status,     // opcional: si viene null, @PrePersist lo deja en AVAILABLE
        Long stationId         // opcional: puede ser null (ON DELETE SET NULL en BD)
		) {}
