package com.unir.bikeshare.backend.payments.dto;

import com.unir.bikeshare.backend.payments.model.PaymentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentWebhookRequest(
		@NotBlank String sandboxReference,
        @NotNull PaymentStatus status
		) {

}
