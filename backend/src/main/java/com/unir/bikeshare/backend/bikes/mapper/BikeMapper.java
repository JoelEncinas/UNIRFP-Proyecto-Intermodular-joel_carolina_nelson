package com.unir.bikeshare.backend.bikes.mapper;

import com.unir.bikeshare.backend.bikes.dto.BikeCreateRequest;
import com.unir.bikeshare.backend.bikes.dto.BikeResponse;
import com.unir.bikeshare.backend.bikes.dto.BikeUpdateRequest;
import com.unir.bikeshare.backend.bikes.model.Bike;
import com.unir.bikeshare.backend.stations.model.Station;

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
	
	public static Bike toEntity(BikeCreateRequest req, Station station) {
	    Bike bike = new Bike();
	    bike.setModel(req.model());
	    bike.setStatus(req.status());
	    bike.setStation(station);
	    return bike;
	}
	
	public static void updateEntity(Bike bike, BikeUpdateRequest req, Station station) {

	    if (req.model() != null) {
	        bike.setModel(req.model());
	    }

	    if (req.status() != null) {
	        bike.setStatus(req.status());
	    }

	    bike.setStation(station);
	}
}
