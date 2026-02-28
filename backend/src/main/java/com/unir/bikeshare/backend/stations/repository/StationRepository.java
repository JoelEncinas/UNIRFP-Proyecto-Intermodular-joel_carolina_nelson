package com.unir.bikeshare.backend.stations.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unir.bikeshare.backend.stations.model.Station;

public interface StationRepository extends JpaRepository<Station, Long>{

}
