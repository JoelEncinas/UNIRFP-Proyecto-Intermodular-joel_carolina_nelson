package com.unir.bikeshare.backend.bookings.dto;

import com.unir.bikeshare.backend.payments.dto.StripeCheckoutSessionResponse;
import com.unir.bikeshare.backend.payments.model.PaymentStatus;

public record BookingCreatePaymentResponse(
        BookingResponse booking,
        BookingUnlockPaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        StripeCheckoutSessionResponse stripe
) {
}
