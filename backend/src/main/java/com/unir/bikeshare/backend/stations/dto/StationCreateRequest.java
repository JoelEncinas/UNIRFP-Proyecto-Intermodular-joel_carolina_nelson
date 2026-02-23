package com.unir.bikeshare.backend.stations.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StationCreateRequest(
		@NotBlank @Size(max = 100) String name,
        @Size(max = 255) String address,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        @Digits(integer = 2, fraction = 8)
        BigDecimal latitude,

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        @Digits(integer = 3, fraction = 8)
        BigDecimal longitude,

        @NotNull @Min(1) Integer capacity
		) {}
