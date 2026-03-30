package com.unir.bikeshare.backend.bookings.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unir.bikeshare.backend.bikes.model.Bike;
import com.unir.bikeshare.backend.bikes.model.BikeStatus;
import com.unir.bikeshare.backend.bikes.repository.BikeRepository;
import com.unir.bikeshare.backend.bookings.dto.BookingCreateRequest;
import com.unir.bikeshare.backend.bookings.dto.BookingResponse;
import com.unir.bikeshare.backend.bookings.dto.BookingUpdateRequest;
import com.unir.bikeshare.backend.bookings.mapper.BookingMapper;
import com.unir.bikeshare.backend.bookings.model.Booking;
import com.unir.bikeshare.backend.bookings.model.BookingStatus;
import com.unir.bikeshare.backend.bookings.repository.BookingRepository;
import com.unir.bikeshare.backend.common.exception.BusinessException;
import com.unir.bikeshare.backend.common.exception.NotFoundException;
import com.unir.bikeshare.backend.stations.model.Station;
import com.unir.bikeshare.backend.stations.repository.StationRepository;
import com.unir.bikeshare.backend.users.model.User;
import com.unir.bikeshare.backend.users.repository.UserRepository;

@Service
@Transactional
public class BookingService {

	private static final List<BookingStatus> ACTIVE_OR_PENDING = List.of(
            BookingStatus.PENDING,
            BookingStatus.ACTIVE
    );
	
	private static final List<BikeStatus> DOCK_OCCUPYING_STATUSES = List.of(
            BikeStatus.AVAILABLE,
            BikeStatus.BOOKED,
            BikeStatus.MAINTENANCE
    );
	
	private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BikeRepository bikeRepository;
    private final StationRepository stationRepository;
    
	public BookingService(BookingRepository bookingRepository, UserRepository userRepository,
			BikeRepository bikeRepository, StationRepository stationRepository) {
		super();
		this.bookingRepository = bookingRepository;
		this.userRepository = userRepository;
		this.bikeRepository = bikeRepository;
		this.stationRepository = stationRepository;
	}
	
	//READ
	
	@Transactional(readOnly = true)
    public BookingResponse getById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        return BookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAll() {
        return bookingRepository.findAll()
                .stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getByUser(Long userId) {
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getByBike(Long bikeId) {
        return bookingRepository.findByBikeId(bikeId)
                .stream()
                .map(BookingMapper::toResponse)
                .toList();
    }
    
    //CREATE
    
    public BookingResponse create(BookingCreateRequest req) {
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Bike bike = bikeRepository.findById(req.bikeId())
                .orElseThrow(() -> new NotFoundException("Bike not found"));

        if (bike.getStatus() != BikeStatus.AVAILABLE) {
            throw new BusinessException("Bike is not available");
        }

        boolean userHasActiveOrPending = bookingRepository.existsByUserIdAndStatusIn(
                user.getId(),
                ACTIVE_OR_PENDING
        );

        if (userHasActiveOrPending) {
            throw new BusinessException("User already has an active or pending booking");
        }

        boolean hasActiveOrPending = bookingRepository.existsByBikeIdAndStatusIn(
                bike.getId(),
                ACTIVE_OR_PENDING
        );

        if (hasActiveOrPending) {
            throw new BusinessException("Bike already has an active or pending booking");
        }

        Booking booking = BookingMapper.toEntity(req, user, bike);
        booking.setPickupStation(bike.getStation());

        // Reservar la bici “ya”
        bike.setStatus(BikeStatus.BOOKED);

        Booking saved = bookingRepository.save(booking);
        return BookingMapper.toResponse(saved);
    }
    
    //UPDATE
    
    public BookingResponse update(Long id, BookingUpdateRequest req) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        // expiryTime (opcional)
        if (req.expiryTime() != null) {
            booking.setExpiryTime(req.expiryTime());
        }

        // status (opcional)
        if (req.status() != null) {
            applyStatusChange(booking, req.status());
        }

        Booking saved = bookingRepository.save(booking);
        return BookingMapper.toResponse(saved);
    }
    
    public BookingResponse returnBike(Long bookingId, Long stationId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BusinessException("Only ACTIVE bookings can be returned");
        }

        Station destinationStation = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found"));

        long occupiedDocks = bikeRepository.countByStationIdAndStatusIn(
                stationId,
                DOCK_OCCUPYING_STATUSES
        );

        if (occupiedDocks >= destinationStation.getCapacity()) {
            throw new BusinessException("Station is full");
        }

        Bike bike = booking.getBike();
        bike.setStation(destinationStation);
        bike.setStatus(BikeStatus.AVAILABLE);
        booking.setDropoffStation(destinationStation);
        booking.setReturnedAt(Instant.now());
        booking.setStatus(BookingStatus.COMPLETED);

        Booking saved = bookingRepository.save(booking);
        return BookingMapper.toResponse(saved);
    }
    
    private void applyStatusChange(Booking booking, BookingStatus newStatus) {
        BookingStatus current = booking.getStatus();
        Bike bike = booking.getBike();

        if (current == newStatus) return;

        switch (newStatus) {
            case ACTIVE -> {
                if (current != BookingStatus.PENDING) {
                    throw new BusinessException("Only PENDING bookings can be activated");
                }
                if (booking.getActivatedAt() == null) {
                    booking.setActivatedAt(Instant.now());
                }
                booking.setStatus(BookingStatus.ACTIVE);
                bike.setStatus(BikeStatus.BUSY);
                bike.setStation(null);
            }
            case COMPLETED -> throw new BusinessException("Use return endpoint to complete an ACTIVE booking");
            case CANCELLED -> {
                if (current != BookingStatus.PENDING) {
                    throw new BusinessException("Only PENDING bookings can be cancelled");
                }
                booking.setStatus(BookingStatus.CANCELLED);
                bike.setStatus(BikeStatus.AVAILABLE);
            }
            case PENDING -> throw new BusinessException("Cannot revert booking status to PENDING");
        }
    }
    
    //DELETE no es necesario, bookings se controlan con estado. 
    
    
}
