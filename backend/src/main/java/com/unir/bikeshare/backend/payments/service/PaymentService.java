package com.unir.bikeshare.backend.payments.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unir.bikeshare.backend.bookings.model.Booking;
import com.unir.bikeshare.backend.bookings.repository.BookingRepository;
import com.unir.bikeshare.backend.common.exception.BusinessException;
import com.unir.bikeshare.backend.common.exception.NotFoundException;
import com.unir.bikeshare.backend.payments.dto.PaymentCreateRequest;
import com.unir.bikeshare.backend.payments.dto.PaymentResponse;
import com.unir.bikeshare.backend.payments.mapper.PaymentMapper;
import com.unir.bikeshare.backend.payments.model.Payment;
import com.unir.bikeshare.backend.payments.model.PaymentMethod;
import com.unir.bikeshare.backend.payments.model.TransactionType;
import com.unir.bikeshare.backend.payments.repository.PaymentRepository;
import com.unir.bikeshare.backend.users.model.User;
import com.unir.bikeshare.backend.users.repository.UserRepository;


@Service
@Transactional
public class PaymentService {
	
	private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            BookingRepository bookingRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }
    
    //READ

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        return PaymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll()
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getByUser(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }
    
    //CREATE

    public PaymentResponse create(PaymentCreateRequest req) {

        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Booking booking = null;
        if (req.bookingId() != null) {
            booking = bookingRepository.findById(req.bookingId())
                    .orElseThrow(() -> new NotFoundException("Booking not found"));
        }

        // Validación extra por seguridad (aunque ya venga validado por DTO)
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than 0");
        }

        // Lógica de balance
        applyWalletRules(user, req.transactionType(), req.paymentMethod(), req.amount());

        // Crear Payment entity
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setBooking(booking);
        payment.setAmount(req.amount());
        payment.setProvider(req.provider());
        payment.setSandboxReference(req.sandboxReference());
        payment.setPaymentMethod(req.paymentMethod());
        payment.setTransactionType(req.transactionType());

        Payment saved = paymentRepository.save(payment);
        return PaymentMapper.toResponse(saved);
    }
    
    //OTHERS
    
    //Modificar el balance (credito de la app) del usuario

    private void applyWalletRules(User user, TransactionType type, PaymentMethod method, BigDecimal amount) {

        if (type == TransactionType.TOP_UP) {
            // Asumimos éxito inmediato por ahora ya que no controlamos estado
            user.setBalance(user.getBalance().add(amount));
            return;
        }

        if (method == PaymentMethod.APP_CREDIT) {
            if (user.getBalance().compareTo(amount) < 0) {
                throw new BusinessException("Insufficient balance");
            }
            user.setBalance(user.getBalance().subtract(amount));
        }
        
        // Si es CREDIT_CARD o PAYPAL: no toca balance (dinero externo)
    }

}
