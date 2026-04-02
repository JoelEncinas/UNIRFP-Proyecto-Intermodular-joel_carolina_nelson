package com.unir.bikeshare.backend.bookings.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.unir.bikeshare.backend.bookings.dto.BookingCreateRequest;
import com.unir.bikeshare.backend.bookings.dto.BookingCreatePaymentResponse;
import com.unir.bikeshare.backend.bookings.dto.BookingReturnRequest;
import com.unir.bikeshare.backend.bookings.dto.BookingResponse;
import com.unir.bikeshare.backend.bookings.dto.BookingStripeSessionRequest;
import com.unir.bikeshare.backend.bookings.dto.BookingUpdateRequest;
import com.unir.bikeshare.backend.bookings.model.BookingStatus;
import com.unir.bikeshare.backend.bookings.service.BookingService;
import com.unir.bikeshare.backend.common.exception.BusinessException;
import com.unir.bikeshare.backend.security.CurrentUserAccessService;
import com.unir.bikeshare.backend.users.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
	private final BookingService bookingService;
    private final CurrentUserAccessService currentUserAccessService;

    public BookingController(BookingService bookingService, CurrentUserAccessService currentUserAccessService) {
        this.bookingService = bookingService;
        this.currentUserAccessService = currentUserAccessService;
    }
    
    @GetMapping
    public List<BookingResponse> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long bikeId,
            Authentication authentication
    ) {
        if (!currentUserAccessService.isAdmin(authentication)) {
            UserResponse currentUser = currentUserAccessService.getCurrentUser(authentication);
            if (userId != null) {
                currentUserAccessService.ensureUserOwnsResource(authentication, userId);
            }

            List<BookingResponse> ownBookings = bookingService.getByUser(currentUser.id());
            if (bikeId == null) {
                return ownBookings;
            }

            return ownBookings.stream()
                    .filter(booking -> Objects.equals(booking.bikeId(), bikeId))
                    .toList();
        }

        if (userId != null) return bookingService.getByUser(userId);
        if (bikeId != null) return bookingService.getByBike(bikeId);
        return bookingService.getAll();
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable Long id, Authentication authentication) {
        BookingResponse booking = bookingService.getById(id);
        ensureCanAccessBooking(authentication, booking);
        return booking;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingCreatePaymentResponse create(@RequestBody @Valid BookingCreateRequest req, Authentication authentication) {
        if (!currentUserAccessService.isAdmin(authentication)) {
            currentUserAccessService.ensureUserOwnsResource(authentication, req.userId());
        }
        return bookingService.create(req);
    }

    @PostMapping("/{bookingId}/stripe/finalize")
    public BookingCreatePaymentResponse finalizeStripeUnlock(
            @PathVariable Long bookingId,
            @RequestBody @Valid BookingStripeSessionRequest req,
            Authentication authentication
    ) {
        if (!Objects.equals(bookingId, req.bookingId())) {
            throw new BusinessException("Path booking id and payload booking id must match");
        }
        ensureCanAccessBooking(authentication, bookingService.getById(bookingId));
        return bookingService.finalizeStripeUnlock(req.bookingId(), req.sessionId());
    }

    @PostMapping("/{bookingId}/stripe/cancel")
    public BookingCreatePaymentResponse cancelStripeUnlock(
            @PathVariable Long bookingId,
            @RequestBody @Valid BookingStripeSessionRequest req,
            Authentication authentication
    ) {
        if (!Objects.equals(bookingId, req.bookingId())) {
            throw new BusinessException("Path booking id and payload booking id must match");
        }
        ensureCanAccessBooking(authentication, bookingService.getById(bookingId));
        return bookingService.cancelStripeUnlock(req.bookingId(), req.sessionId());
    }

    @PutMapping("/{id}")
    public BookingResponse update(@PathVariable Long id, @RequestBody BookingUpdateRequest req, Authentication authentication) {
        ensureCanAccessBooking(authentication, bookingService.getById(id));
        return bookingService.update(id, req);
    }

    // Endpoints expresivos
    
    //cambiar reserva a activa
    @PostMapping("/{id}/activate")
    public BookingResponse activate(@PathVariable Long id, Authentication authentication) {
        ensureCanAccessBooking(authentication, bookingService.getById(id));
        return bookingService.update(id, new BookingUpdateRequest(BookingStatus.ACTIVE, null));
    }

    //cambiar reserva a cancelada
    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, Authentication authentication) {
        ensureCanAccessBooking(authentication, bookingService.getById(id));
        return bookingService.update(id, new BookingUpdateRequest(BookingStatus.CANCELLED, null));
    }
    
    @PostMapping("/{id}/return")
    public BookingResponse returnBike(
            @PathVariable Long id,
            @Valid @RequestBody BookingReturnRequest request,
            Authentication authentication
    ) {
        ensureCanAccessBooking(authentication, bookingService.getById(id));
        return bookingService.returnBike(id, request.stationId());
    }

    private void ensureCanAccessBooking(Authentication authentication, BookingResponse booking) {
        currentUserAccessService.ensureUserOwnsResource(authentication, booking.userId());
    }
}
