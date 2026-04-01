package com.unir.bikeshare.backend.payments.dto;

import jakarta.validation.constraints.NotBlank;

public record StripeCheckoutSessionCancelRequest(
        @NotBlank String sessionId
) {
}
