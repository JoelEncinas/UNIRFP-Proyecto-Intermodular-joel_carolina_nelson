package com.unir.bikeshare.backend.stations.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unir.bikeshare.backend.stations.dto.StationResponse;
import com.unir.bikeshare.backend.stations.service.StationService;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public List<StationResponse> getAll() {
        return stationService.getAll();
    }

    @GetMapping("/{id}")
    public StationResponse getById(@PathVariable Long id) {
        return stationService.getById(id);
    }
}
