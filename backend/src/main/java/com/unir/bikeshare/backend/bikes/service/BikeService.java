package com.unir.bikeshare.backend.bikes.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unir.bikeshare.backend.bikes.dto.BikeCreateRequest;
import com.unir.bikeshare.backend.bikes.dto.BikeResponse;
import com.unir.bikeshare.backend.bikes.dto.BikeUpdateRequest;
import com.unir.bikeshare.backend.bikes.mapper.BikeMapper;
import com.unir.bikeshare.backend.bikes.model.Bike;
import com.unir.bikeshare.backend.bikes.model.BikeStatus;
import com.unir.bikeshare.backend.bikes.repository.BikeRepository;
import com.unir.bikeshare.backend.common.exception.NotFoundException;
import com.unir.bikeshare.backend.stations.model.Station;
import com.unir.bikeshare.backend.stations.repository.StationRepository;

@Service
@Transactional
public class BikeService {
	
	private final BikeRepository bikeRepository;
	private final StationRepository stationRepository;
	
	public BikeService(BikeRepository bikeRepository, StationRepository stationRepository) {
		super();
		this.bikeRepository = bikeRepository;
		this.stationRepository = stationRepository;
	}
	
	//READ
	
	@Transactional(readOnly = true)
    public BikeResponse getById(Long id) {
        Bike bike = bikeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bike not found"));
        return BikeMapper.toResponse(bike);
    }
	
	@Transactional(readOnly = true)
    public List<BikeResponse> getAll() {
        return bikeRepository.findAll()
                .stream()
                .map(BikeMapper::toResponse)
                .toList();
    }
	
	@Transactional(readOnly = true)
    public List<BikeResponse> getByStatus(BikeStatus status) {
        return bikeRepository.findByStatus(status)
                .stream()
                .map(BikeMapper::toResponse)
                .toList();
    }
	
	@Transactional(readOnly = true)
    public List<BikeResponse> getByStation(Long stationId) {
        return bikeRepository.findByStationId(stationId)
                .stream()
                .map(BikeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BikeResponse> getByStationAndStatus(Long stationId, BikeStatus status) {
        return bikeRepository.findByStationIdAndStatus(stationId, status)
                .stream()
                .map(BikeMapper::toResponse)
                .toList();
    }
	
	//CREATE
	
	public BikeResponse create(BikeCreateRequest req) {
        Station station = null;
        if (req.stationId() != null) {
            station = stationRepository.findById(req.stationId())
                    .orElseThrow(() -> new NotFoundException("Station not found"));
        }

        Bike bike = BikeMapper.toEntity(req, station);
        Bike saved = bikeRepository.save(bike);
        return BikeMapper.toResponse(saved);
    }
	
	//UPDATE
	
	public BikeResponse update(Long id, BikeUpdateRequest req) {
        Bike bike = bikeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bike not found"));

        Station station = null;
        if (req.stationId() != null) {
            station = stationRepository.findById(req.stationId())
                    .orElseThrow(() -> new NotFoundException("Station not found"));
        }
        // Si req.stationId() == null -> se desasigna (según tu mapper actual)

        BikeMapper.updateEntity(bike, req, station);
        Bike saved = bikeRepository.save(bike);
        return BikeMapper.toResponse(saved);
    }
	
	//DELETE 
	
	public void delete(Long id) {
        if (!bikeRepository.existsById(id)) {
            throw new NotFoundException("Bike not found");
        }
        bikeRepository.deleteById(id);
    }


}
