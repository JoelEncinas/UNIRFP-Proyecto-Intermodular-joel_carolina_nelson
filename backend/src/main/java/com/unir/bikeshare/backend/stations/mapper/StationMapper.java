package com.unir.bikeshare.backend.stations.mapper;

import com.unir.bikeshare.backend.stations.dto.StationResponse;
import com.unir.bikeshare.backend.stations.model.Station;

public class StationMapper {
	
	private StationMapper() {}

	public static StationResponse toResponse(Station station) {
		return new StationResponse(station.getId(), station.getName(), station.getAddress(), station.getLatitude(),
				station.getLongitude(), station.getCapacity());
	}
}
