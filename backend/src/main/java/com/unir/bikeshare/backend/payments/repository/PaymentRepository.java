package com.unir.bikeshare.backend.payments.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unir.bikeshare.backend.payments.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	List<Payment> findByUserId(Long userId);

    List<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findByBookingIdAndSandboxReference(Long bookingId, String sandboxReference);

    Optional<Payment> findBySandboxReference(String sandboxReference);
}
