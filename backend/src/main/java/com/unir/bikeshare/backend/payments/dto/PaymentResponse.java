package com.unir.bikeshare.backend.payments.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.unir.bikeshare.backend.payments.model.PaymentMethod;
import com.unir.bikeshare.backend.payments.model.PaymentStatus;
import com.unir.bikeshare.backend.payments.model.TransactionType;

public record PaymentResponse(
		Long id,

        Long userId,
        String username,

        Long bookingId,

        BigDecimal amount,
        String provider,
        String sandboxReference,

        PaymentMethod paymentMethod,
        TransactionType transactionType,
        PaymentStatus paymentStatus,

        Instant paymentDate
		) {

}
