package com.unir.bikeshare.backend.bookings.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
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
import com.unir.bikeshare.backend.bookings.dto.BookingReturnRequest;
import com.unir.bikeshare.backend.bookings.dto.BookingResponse;
import com.unir.bikeshare.backend.bookings.dto.BookingUpdateRequest;
import com.unir.bikeshare.backend.bookings.model.BookingStatus;
import com.unir.bikeshare.backend.bookings.service.BookingService;
import com.unir.bikeshare.backend.users.dto.UserResponse;
import com.unir.bikeshare.backend.users.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
	private final BookingService bookingService;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }
    
    @GetMapping
    public List<BookingResponse> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long bikeId,
            Authentication authentication
    ) {
        if (!isAdmin(authentication)) {
            UserResponse currentUser = getCurrentUser(authentication);
            if (userId != null && !Objects.equals(userId, currentUser.id())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own bookings");
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
    public BookingResponse create(@RequestBody @Valid BookingCreateRequest req, Authentication authentication) {
        if (!isAdmin(authentication)) {
            UserResponse currentUser = getCurrentUser(authentication);
            if (!Objects.equals(req.userId(), currentUser.id())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create bookings for yourself");
            }
        }
        return bookingService.create(req);
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

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private UserResponse getCurrentUser(Authentication authentication) {
        return userService.getByUsername(authentication.getName());
    }

    private void ensureCanAccessBooking(Authentication authentication, BookingResponse booking) {
        if (isAdmin(authentication)) {
            return;
        }

        UserResponse currentUser = getCurrentUser(authentication);
        if (!Objects.equals(booking.userId(), currentUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own bookings");
        }
    }
}
