package com.unir.bikeshare.backend.bikes.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.unir.bikeshare.backend.bikes.dto.BikeCreateRequest;
import com.unir.bikeshare.backend.bikes.dto.BikeResponse;
import com.unir.bikeshare.backend.bikes.dto.BikeUpdateRequest;
import com.unir.bikeshare.backend.bikes.model.BikeStatus;
import com.unir.bikeshare.backend.bikes.service.BikeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bikes")
public class BikeController {
	private final BikeService bikeService;

    public BikeController(BikeService bikeService) {
        this.bikeService = bikeService;
    }

    @GetMapping
    public List<BikeResponse> getAll(
            @RequestParam(required = false) BikeStatus status,
            @RequestParam(required = false) Long stationId
    ) {
        if (status != null && stationId != null) {
            return bikeService.getByStation(stationId).stream()
                    .filter(b -> b.status() == status)
                    .toList();
        }
        if (status != null) return bikeService.getByStatus(status);
        if (stationId != null) return bikeService.getByStation(stationId);
        return bikeService.getAll();
    }

    @GetMapping("/{id}")
    public BikeResponse getById(@PathVariable Long id) {
        return bikeService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BikeResponse create(@RequestBody @Valid BikeCreateRequest req) {
        return bikeService.create(req);
    }

    @PutMapping("/{id}")
    public BikeResponse update(@PathVariable Long id, @RequestBody @Valid BikeUpdateRequest req) {
        return bikeService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bikeService.delete(id);
    }
}
