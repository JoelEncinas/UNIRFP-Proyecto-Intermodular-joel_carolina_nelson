package com.unir.bikeshare.backend.bikes.mapper;

import com.unir.bikeshare.backend.bikes.dto.BikeResponse;
import com.unir.bikeshare.backend.bikes.model.Bike;

public class BikeMapper {

	private BikeMapper() {}
	
	public static BikeResponse toResponse(Bike bike) {
        Long stationId = (bike.getStation() == null) ? null : bike.getStation().getId();
        return new BikeResponse(
                bike.getId(),
                bike.getModel(),
                bike.getStatus(),
                stationId
        );
    }
}
