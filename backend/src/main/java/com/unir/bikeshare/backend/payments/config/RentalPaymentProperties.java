package com.unir.bikeshare.backend.payments.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payments.rental")
public class RentalPaymentProperties {

    private BigDecimal unlockFee = BigDecimal.ONE;

    public BigDecimal getUnlockFee() {
        return unlockFee;
    }

    public void setUnlockFee(BigDecimal unlockFee) {
        this.unlockFee = unlockFee;
    }
}
