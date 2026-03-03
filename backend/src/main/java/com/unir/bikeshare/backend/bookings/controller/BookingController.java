package com.unir.bikeshare.backend.bookings.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import com.unir.bikeshare.backend.bookings.dto.BookingResponse;
import com.unir.bikeshare.backend.bookings.dto.BookingUpdateRequest;
import com.unir.bikeshare.backend.bookings.model.BookingStatus;
import com.unir.bikeshare.backend.bookings.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
	private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }
    
    @GetMapping
    public List<BookingResponse> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long bikeId
    ) {
        if (userId != null) return bookingService.getByUser(userId);
        if (bikeId != null) return bookingService.getByBike(bikeId);
        return bookingService.getAll();
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable Long id) {
        return bookingService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@RequestBody @Valid BookingCreateRequest req) {
        return bookingService.create(req);
    }

    @PutMapping("/{id}")
    public BookingResponse update(@PathVariable Long id, @RequestBody BookingUpdateRequest req) {
        return bookingService.update(id, req);
    }

    // Endpoints expresivos
    
    //cambiar reserva a activa
    @PostMapping("/{id}/activate")
    public BookingResponse activate(@PathVariable Long id) {
        return bookingService.update(id, new BookingUpdateRequest(BookingStatus.ACTIVE, null));
    }

    //cambiar reserva a empezada
    @PostMapping("/{id}/complete")
    public BookingResponse complete(@PathVariable Long id) {
        return bookingService.update(id, new BookingUpdateRequest(BookingStatus.COMPLETED, null));
    }

    //cambiar reserva a cancelada
    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id) {
        return bookingService.update(id, new BookingUpdateRequest(BookingStatus.CANCELLED, null));
    }
}
