package com.unir.bikeshare.backend.payments.stripe;

public record StripeCheckoutSessionData(
        String sessionId,
        String checkoutUrl
) {
}
