package com.unir.bikeshare.backend.payments.dto;

import java.math.BigDecimal;

public record PaymentConfigResponse(
        BigDecimal unlockFee,
        String currency,
        BigDecimal minTopUpAmount
) {
}
