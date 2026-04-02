package com.unir.bikeshare.backend.payments.dto;

import java.math.BigDecimal;

import com.unir.bikeshare.backend.payments.model.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record PaymentCheckoutSessionCreateRequest(
        @NotNull Long userId,
        @NotNull
        @DecimalMin(value = "0.50")
        @Digits(integer = 8, fraction = 2)
        BigDecimal amount,
        @NotNull TransactionType transactionType
) {
}
