package com.unir.bikeshare.backend.payments.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.unir.bikeshare.backend.payments.dto.PaymentCreateRequest;
import com.unir.bikeshare.backend.payments.dto.PaymentResponse;
import com.unir.bikeshare.backend.payments.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
	private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentResponse> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long bookingId
    ) {
        if (userId != null) return paymentService.getByUser(userId);
        if (bookingId != null) return paymentService.getByBooking(bookingId);
        return paymentService.getAll();
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable Long id) {
        return paymentService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@RequestBody @Valid PaymentCreateRequest req) {
        return paymentService.create(req);
    }
}
