package com.unir.bikeshare.backend.payments.mapper;

import com.unir.bikeshare.backend.payments.dto.PaymentResponse;
import com.unir.bikeshare.backend.payments.model.Payment;

public class PaymentMapper {
	private PaymentMapper() {}

    public static PaymentResponse toResponse(Payment payment) {
        Long userId = null;
        String username = null;

        Long bookingId = null;

        if (payment.getUser() != null) {
            userId = payment.getUser().getId();
            username = payment.getUser().getUsername();
        }

        if (payment.getBooking() != null) {
            bookingId = payment.getBooking().getId();
        }

        return new PaymentResponse(
                payment.getId(),
                userId,
                username,
                bookingId,
                payment.getAmount(),
                payment.getProvider(),
                payment.getSandboxReference(),
                payment.getPaymentMethod(),
                payment.getTransactionType(),
                payment.getPaymentStatus(),
                payment.getPaymentDate()
        );
    }
}
