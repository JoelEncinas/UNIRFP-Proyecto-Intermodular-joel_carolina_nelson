package com.unir.bikeshare.backend.stations.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unir.bikeshare.backend.common.exception.NotFoundException;
import com.unir.bikeshare.backend.stations.dto.StationResponse;
import com.unir.bikeshare.backend.stations.mapper.StationMapper;
import com.unir.bikeshare.backend.stations.model.Station;
import com.unir.bikeshare.backend.stations.repository.StationRepository;

@Service
@Transactional
public class StationService {

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public List<StationResponse> getAll() {
        return stationRepository.findAll()
                .stream()
                .map(StationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StationResponse getById(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found"));
        return StationMapper.toResponse(station);
    }
}
