package com.unir.bikeshare.backend.bikes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unir.bikeshare.backend.bikes.model.Bike;
import com.unir.bikeshare.backend.bikes.model.BikeStatus;

public interface BikeRepository extends JpaRepository<Bike, Long>{
	List<Bike> findByStatus(BikeStatus status);

    List<Bike> findByStationId(Long stationId);

    List<Bike> findByStationIdAndStatus(Long stationId, BikeStatus status);

    long countByStationIdAndStatus(Long stationId, BikeStatus status);

    boolean existsByStationId(Long stationId);
}
