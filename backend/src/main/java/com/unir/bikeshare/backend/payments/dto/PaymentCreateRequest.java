package com.unir.bikeshare.backend.payments.dto;

import java.math.BigDecimal;

import com.unir.bikeshare.backend.payments.model.PaymentMethod;
import com.unir.bikeshare.backend.payments.model.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentCreateRequest(
		@NotNull Long userId,
        Long bookingId, // opcional (en BD es NULL)

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 8, fraction = 2) // DECIMAL(10,2) -> 8 enteros + 2 decimales
        BigDecimal amount,

        @Size(max = 50) String provider,
        @Size(max = 100) String sandboxReference,

        @NotNull PaymentMethod paymentMethod,
        @NotNull TransactionType transactionType
        ) {

}
