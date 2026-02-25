package com.unir.bikeshare.backend.stations.dto;

import java.math.BigDecimal;

public record StationResponse(
		Long id,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer capacity
		) {}
