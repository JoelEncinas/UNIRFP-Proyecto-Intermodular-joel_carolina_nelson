package com.unir.bikeshare.backend.payments.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unir.bikeshare.backend.payments.model.Payment;
import com.unir.bikeshare.backend.payments.model.TransactionType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	List<Payment> findByUserId(Long userId);

    List<Payment> findByBookingId(Long bookingId);

    List<Payment> findByTransactionType(TransactionType transactionType);

    List<Payment> findByUserIdAndPaymentDateBetween(Long userId, Instant from, Instant to);
}
