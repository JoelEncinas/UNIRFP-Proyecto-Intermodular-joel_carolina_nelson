package com.unir.bikeshare.backend.bookings.mapper;

import com.unir.bikeshare.backend.bookings.dto.BookingResponse;
import com.unir.bikeshare.backend.bookings.model.Booking;

public class BookingMapper {

	private BookingMapper() {
	}

	public static BookingResponse toResponse(Booking booking) {
		Long userId = null;
		String username = null;

		Long bikeId = null;
		String bikeModel = null;

		if (booking.getUser() != null) {
			userId = booking.getUser().getId();
			username = booking.getUser().getUsername();
		}

		if (booking.getBike() != null) {
			bikeId = booking.getBike().getId();
			bikeModel = booking.getBike().getModel();
		}

		return new BookingResponse(booking.getId(), userId, username, bikeId, bikeModel, booking.getStartTime(),
				booking.getExpiryTime(), booking.getStatus());
	}

}
