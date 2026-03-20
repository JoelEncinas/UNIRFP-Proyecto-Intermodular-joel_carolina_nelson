package com.unir.bikeshare.backend.bookings.model;

import java.time.Instant;

import com.unir.bikeshare.backend.bikes.model.Bike;
import com.unir.bikeshare.backend.stations.model.Station;
import com.unir.bikeshare.backend.users.model.User;

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
@Table(name = "bookings")
public class Booking {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK user_id -> users(id) ON DELETE CASCADE
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // FK bike_id -> bikes(id) ON DELETE CASCADE
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bike_id", nullable = false)
    private Bike bike;

    // Snapshot of the station where the ride was picked up.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_station_id")
    private Station pickupStation;

    // Snapshot of the station where the ride was returned.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dropoff_station_id")
    private Station dropoffStation;

    // start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    // expiry_time TIMESTAMP
    @Column(name = "expiry_time")
    private Instant expiryTime;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @PrePersist
    private void prePersist() {
        if (startTime == null) startTime = Instant.now();
        if (status == null) status = BookingStatus.PENDING;
    }

    // ===== getters/setters =====

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Bike getBike() { return bike; }
    public void setBike(Bike bike) { this.bike = bike; }

    public Station getPickupStation() { return pickupStation; }
    public void setPickupStation(Station pickupStation) { this.pickupStation = pickupStation; }

    public Station getDropoffStation() { return dropoffStation; }
    public void setDropoffStation(Station dropoffStation) { this.dropoffStation = dropoffStation; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getExpiryTime() { return expiryTime; }
    public void setExpiryTime(Instant expiryTime) { this.expiryTime = expiryTime; }

    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }

    public Instant getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
}
