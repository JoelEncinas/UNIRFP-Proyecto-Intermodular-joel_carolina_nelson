package com.unir.bikeshare.backend.payments.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.unir.bikeshare.backend.bookings.model.Booking;
import com.unir.bikeshare.backend.users.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK user_id -> users(id) ON DELETE CASCADE
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // FK booking_id -> bookings(id) ON DELETE SET NULL (nullable)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // DECIMAL(10,2) NOT NULL
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    // provider VARCHAR(50)
    @Column(name = "provider", length = 50)
    private String provider;

    // sandbox_reference VARCHAR(100)
    @Column(name = "sandbox_reference", length = 100)
    private String sandboxReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    // TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    @Column(name = "payment_date", nullable = false, updatable = false)
    private Instant paymentDate;

    @PrePersist
    private void prePersist() {
        if (paymentDate == null) paymentDate = Instant.now();
    }

    // ===== getters / setters =====

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getSandboxReference() { return sandboxReference; }
    public void setSandboxReference(String sandboxReference) { this.sandboxReference = sandboxReference; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public Instant getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Instant paymentDate) { this.paymentDate = paymentDate; }
}
