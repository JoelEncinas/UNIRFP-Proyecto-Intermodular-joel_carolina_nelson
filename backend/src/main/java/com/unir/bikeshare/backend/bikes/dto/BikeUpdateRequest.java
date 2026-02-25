package com.unir.bikeshare.backend.bikes.dto;

import com.unir.bikeshare.backend.bikes.model.BikeStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BikeUpdateRequest(
		 @NotBlank @Size(max = 50) String model,
	     BikeStatus status,  // opcional
	     Long stationId      // opcional
		) {

}
