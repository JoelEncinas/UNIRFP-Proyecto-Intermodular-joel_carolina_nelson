package com.unir.bikeshare.backend.bookings.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unir.bikeshare.backend.bookings.model.Booking;
import com.unir.bikeshare.backend.bookings.model.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long>{
	List<Booking> findByUserId(Long userId);

    List<Booking> findByBikeId(Long bikeId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByStatusAndExpiryTimeBefore(BookingStatus status, Instant cutoff);

    Optional<Booking> findFirstByBikeIdAndStatusInOrderByStartTimeDesc(Long bikeId, List<BookingStatus> statuses);

    boolean existsByBikeIdAndStatusIn(Long bikeId, List<BookingStatus> statuses);

    boolean existsByUserIdAndStatusIn(Long userId, List<BookingStatus> statuses);
}
