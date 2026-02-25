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
import com.unir.bikeshare.backend.users.model.User;
import com.unir.bikeshare.backend.users.repository.UserRepository;

@Service
@Transactional
public class BookingService {

	private static final List<BookingStatus> ACTIVE_OR_PENDING = List.of(
            BookingStatus.PENDING,
            BookingStatus.ACTIVE
    );
	
	private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BikeRepository bikeRepository;
    
	public BookingService(BookingRepository bookingRepository, UserRepository userRepository,
			BikeRepository bikeRepository) {
		super();
		this.bookingRepository = bookingRepository;
		this.userRepository = userRepository;
		this.bikeRepository = bikeRepository;
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

        boolean hasActiveOrPending = bookingRepository.existsByBikeIdAndStatusIn(
                bike.getId(),
                ACTIVE_OR_PENDING
        );

        if (hasActiveOrPending) {
            throw new BusinessException("Bike already has an active or pending booking");
        }

        Booking booking = BookingMapper.toEntity(req, user, bike);

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
    
    public int expirePendingBefore(Instant cutoff) {
        List<Booking> expired = bookingRepository.findByStatusAndExpiryTimeBefore(BookingStatus.PENDING, cutoff);

        for (Booking b : expired) {
            b.setStatus(BookingStatus.CANCELLED);
            b.getBike().setStatus(BikeStatus.AVAILABLE);
        }

        bookingRepository.saveAll(expired);
        return expired.size();
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
                booking.setStatus(BookingStatus.ACTIVE);
                bike.setStatus(BikeStatus.BUSY);
            }
            case COMPLETED -> {
                if (current != BookingStatus.ACTIVE) {
                    throw new BusinessException("Only ACTIVE bookings can be completed");
                }
                booking.setStatus(BookingStatus.COMPLETED);
                bike.setStatus(BikeStatus.AVAILABLE);
            }
            case CANCELLED -> {
                if (current != BookingStatus.PENDING && current != BookingStatus.ACTIVE) {
                    throw new BusinessException("Only PENDING or ACTIVE bookings can be cancelled");
                }
                booking.setStatus(BookingStatus.CANCELLED);
                bike.setStatus(BikeStatus.AVAILABLE);
            }
            case PENDING -> throw new BusinessException("Cannot revert booking status to PENDING");
        }
    }
    
    //DELETE no es necesario, bookings se controlan con estado. 
    
    
}
