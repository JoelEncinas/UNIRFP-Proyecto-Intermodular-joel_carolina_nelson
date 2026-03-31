package com.unir.bikeshare.backend.payments.stripe;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.unir.bikeshare.backend.common.exception.BusinessException;

@Component
public class StripeGateway {

    public StripeCheckoutSessionData createTopUpCheckoutSession(
            Long userId,
            BigDecimal amount,
            String currency,
            String successUrl,
            String cancelUrl
    ) {
        long amountInCents;
        try {
            amountInCents = amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException ex) {
            throw new BusinessException("Invalid amount format");
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("userId", String.valueOf(userId))
                .putMetadata("transactionType", "TOP_UP")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("BikeShare wallet top-up")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        try {
            Session session = Session.create(params);
            return new StripeCheckoutSessionData(session.getId(), session.getUrl());
        } catch (StripeException ex) {
            throw new BusinessException("Unable to create Stripe checkout session");
        }
    }

    public Event constructWebhookEvent(String payload, String signatureHeader, String webhookSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new BusinessException("Stripe webhook secret is not configured");
        }
        try {
            return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new BusinessException("Invalid Stripe signature");
        }
    }
}
