package com.unir.bikeshare.backend.payments.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.unir.bikeshare.backend.payments.dto.PaymentConfigResponse;
import com.unir.bikeshare.backend.payments.dto.PaymentResponse;
import com.unir.bikeshare.backend.payments.dto.StripeCheckoutSessionCancelRequest;
import com.unir.bikeshare.backend.payments.dto.StripeCheckoutSessionCreateRequest;
import com.unir.bikeshare.backend.payments.dto.StripeCheckoutSessionResponse;
import com.unir.bikeshare.backend.payments.service.PaymentService;
import com.unir.bikeshare.backend.security.CurrentUserAccessService;
import com.unir.bikeshare.backend.users.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
	private final PaymentService paymentService;
    private final CurrentUserAccessService currentUserAccessService;

    public PaymentController(PaymentService paymentService, CurrentUserAccessService currentUserAccessService) {
        this.paymentService = paymentService;
        this.currentUserAccessService = currentUserAccessService;
    }

    @GetMapping("/config")
    public PaymentConfigResponse getConfig() {
        return paymentService.getPaymentConfig();
    }

    @GetMapping
    public List<PaymentResponse> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long bookingId,
            Authentication authentication
    ) {
        if (!currentUserAccessService.isAdmin(authentication)) {
            UserResponse currentUser = currentUserAccessService.getCurrentUser(authentication);

            if (userId != null) {
                currentUserAccessService.ensureUserOwnsResource(authentication, userId);
            }

            List<PaymentResponse> ownPayments = paymentService.getByUser(currentUser.id());
            if (bookingId == null) {
                return ownPayments;
            }

            return ownPayments.stream()
                    .filter(payment -> Objects.equals(payment.bookingId(), bookingId))
                    .toList();
        }

        if (userId != null) return paymentService.getByUser(userId);
        if (bookingId != null) return paymentService.getByBooking(bookingId);
        return paymentService.getAll();
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable Long id, Authentication authentication) {
        PaymentResponse payment = paymentService.getById(id);
        currentUserAccessService.ensureUserOwnsResource(authentication, payment.userId());
        return payment;
    }

    @PostMapping("/stripe/checkout-session")
    @ResponseStatus(HttpStatus.CREATED)
    public StripeCheckoutSessionResponse createStripeCheckoutSession(
            @RequestBody @Valid StripeCheckoutSessionCreateRequest req,
            Authentication authentication
    ) {
        boolean isAdmin = currentUserAccessService.isAdmin(authentication);
        UserResponse currentUser = currentUserAccessService.getCurrentUser(authentication);
        if (!isAdmin) {
            currentUserAccessService.ensureUserOwnsResource(authentication, req.userId());
        }
        return paymentService.createStripeCheckoutSession(req, currentUser.id(), isAdmin);
    }

    @PostMapping("/stripe/cancel")
    public ResponseEntity<Void> cancelStripeCheckoutSession(
            @RequestBody @Valid StripeCheckoutSessionCancelRequest req,
            Authentication authentication
    ) {
        boolean isAdmin = currentUserAccessService.isAdmin(authentication);
        UserResponse currentUser = currentUserAccessService.getCurrentUser(authentication);
        paymentService.cancelStripeCheckoutSession(req.sessionId(), currentUser.id(), isAdmin);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        paymentService.processStripeWebhook(payload, signatureHeader);
        return ResponseEntity.ok().build();
    }
}
