package com.unir.bikeshare.backend.bookings.mapper;

import com.unir.bikeshare.backend.bikes.model.Bike;
import com.unir.bikeshare.backend.bookings.dto.BookingCreateRequest;
import com.unir.bikeshare.backend.bookings.dto.BookingResponse;
import com.unir.bikeshare.backend.bookings.model.Booking;
import com.unir.bikeshare.backend.users.model.User;

public class BookingMapper {

	private BookingMapper() {
	}

	public static BookingResponse toResponse(Booking booking) {
		Long userId = null;
		String username = null;

		Long bikeId = null;
		String bikeModel = null;
		Long pickupStationId = null;
		String pickupStationName = null;
		Long dropoffStationId = null;
		String dropoffStationName = null;

		if (booking.getUser() != null) {
			userId = booking.getUser().getId();
			username = booking.getUser().getUsername();
		}

		if (booking.getBike() != null) {
			bikeId = booking.getBike().getId();
			bikeModel = booking.getBike().getModel();
		}

		if (booking.getPickupStation() != null) {
			pickupStationId = booking.getPickupStation().getId();
			pickupStationName = booking.getPickupStation().getName();
		}

		if (booking.getDropoffStation() != null) {
			dropoffStationId = booking.getDropoffStation().getId();
			dropoffStationName = booking.getDropoffStation().getName();
		}

		return new BookingResponse(
				booking.getId(),
				userId,
				username,
				bikeId,
				bikeModel,
				pickupStationId,
				pickupStationName,
				dropoffStationId,
				dropoffStationName,
				booking.getStartTime(),
				booking.getExpiryTime(),
				booking.getActivatedAt(),
				booking.getReturnedAt(),
				booking.getStatus());
	}
	
	public static Booking toEntity(BookingCreateRequest req, User user, Bike bike) {

	    Booking booking = new Booking();

	    booking.setUser(user);
	    booking.setBike(bike);

	    booking.setExpiryTime(req.expiryTime()); 
	    // puede ser null, no pasa nada

	    // No seteamos:
	    // startTime → lo pone @PrePersist
	    // status → lo pone @PrePersist (normalmente PENDING)

	    return booking;
	}

}
