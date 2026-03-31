package com.unir.bikeshare.backend.payments.dto;

import com.unir.bikeshare.backend.payments.model.PaymentStatus;

public record StripeCheckoutSessionResponse(
        Long paymentId,
        String sessionId,
        String checkoutUrl,
        PaymentStatus paymentStatus
) {
}
