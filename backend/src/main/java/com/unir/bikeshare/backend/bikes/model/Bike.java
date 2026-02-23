package com.unir.bikeshare.backend.bikes.model;

import com.unir.bikeshare.backend.stations.model.Station;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "bikes")
public class Bike {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // En SQL: model VARCHAR(50)
    @Column(name="model", length = 50)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    private BikeStatus status = BikeStatus.AVAILABLE;

    // FK station_id -> stations(id) ON DELETE SET NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @PrePersist
    private void prePersist() {
        if (status == null) status = BikeStatus.AVAILABLE;
    }

    // ===== getters/setters =====

    public Long getId() { return id; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public BikeStatus getStatus() { return status; }
    public void setStatus(BikeStatus status) { this.status = status; }

    public Station getStation() { return station; }
    public void setStation(Station station) { this.station = station; }
}
