package com.unir.bikeshare.backend.payments.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.unir.bikeshare.backend.bookings.model.Booking;
import com.unir.bikeshare.backend.bookings.repository.BookingRepository;
import com.unir.bikeshare.backend.common.exception.BusinessException;
import com.unir.bikeshare.backend.common.exception.NotFoundException;
import com.unir.bikeshare.backend.payments.config.StripeProperties;
import com.unir.bikeshare.backend.payments.dto.PaymentCreateRequest;
import com.unir.bikeshare.backend.payments.dto.PaymentResponse;
import com.unir.bikeshare.backend.payments.dto.PaymentWebhookRequest;
import com.unir.bikeshare.backend.payments.dto.StripeCheckoutSessionCreateRequest;
import com.unir.bikeshare.backend.payments.dto.StripeCheckoutSessionResponse;
import com.unir.bikeshare.backend.payments.mapper.PaymentMapper;
import com.unir.bikeshare.backend.payments.model.Payment;
import com.unir.bikeshare.backend.payments.model.PaymentMethod;
import com.unir.bikeshare.backend.payments.model.PaymentStatus;
import com.unir.bikeshare.backend.payments.model.TransactionType;
import com.unir.bikeshare.backend.payments.repository.PaymentRepository;
import com.unir.bikeshare.backend.payments.stripe.StripeCheckoutSessionData;
import com.unir.bikeshare.backend.payments.stripe.StripeGateway;
import com.unir.bikeshare.backend.users.model.User;
import com.unir.bikeshare.backend.users.repository.UserRepository;

@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal STRIPE_MIN_TOP_UP_AMOUNT = new BigDecimal("0.50");

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final StripeGateway stripeGateway;
    private final StripeProperties stripeProperties;

    public PaymentService(
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            BookingRepository bookingRepository,
            StripeGateway stripeGateway,
            StripeProperties stripeProperties
    ) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.stripeGateway = stripeGateway;
        this.stripeProperties = stripeProperties;
    }

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

    public PaymentResponse create(PaymentCreateRequest req, Long requesterUserId, boolean requesterAdmin) {
        if (!requesterAdmin && !Objects.equals(req.userId(), requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create payments for yourself");
        }
        return createInternal(req);
    }

    public StripeCheckoutSessionResponse createStripeCheckoutSession(
            StripeCheckoutSessionCreateRequest req,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        if (!requesterAdmin && !Objects.equals(req.userId(), requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create payments for yourself");
        }

        if (req.amount() == null || req.amount().compareTo(STRIPE_MIN_TOP_UP_AMOUNT) < 0) {
            throw new BusinessException("Amount must be at least 0.50");
        }

        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        String successUrl = stripeProperties.getSuccessUrl();
        String cancelUrl = stripeProperties.getCancelUrl();
        if (successUrl == null || successUrl.isBlank() || cancelUrl == null || cancelUrl.isBlank()) {
            throw new BusinessException("Stripe success/cancel URLs are not configured");
        }

        String currency = normalizeCurrency(stripeProperties.getCurrency());

        StripeCheckoutSessionData sessionData = stripeGateway.createTopUpCheckoutSession(
                user.getId(),
                req.amount(),
                currency,
                successUrl,
                cancelUrl
        );

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setBooking(null);
        payment.setAmount(req.amount());
        payment.setProvider("stripe");
        payment.setSandboxReference(sessionData.sessionId());
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setTransactionType(TransactionType.TOP_UP);
        payment.setPaymentStatus(PaymentStatus.PENDING);

        Payment saved = paymentRepository.save(payment);

        return new StripeCheckoutSessionResponse(
                saved.getId(),
                sessionData.sessionId(),
                sessionData.checkoutUrl(),
                saved.getPaymentStatus()
        );
    }

    public void processStripeWebhook(String payload, String signatureHeader) {
        Event event = stripeGateway.constructWebhookEvent(
                payload,
                signatureHeader,
                stripeProperties.getWebhookSecret()
        );

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                Session session = deserializeEventObject(event, Session.class);
                if (session != null) {
                    applyCheckoutSessionCompleted(session);
                }
            }
            case "checkout.session.expired" -> {
                Session session = deserializeEventObject(event, Session.class);
                if (session != null) {
                    markAsFailedIfPending(session.getId());
                }
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent paymentIntent = deserializeEventObject(event, PaymentIntent.class);
                if (paymentIntent != null) {
                    markFromFailedPaymentIntent(paymentIntent);
                }
            }
            default -> log.debug("Stripe webhook event ignored: {}", event.getType());
        }
    }

    public PaymentResponse confirmWebhook(PaymentWebhookRequest req) {
        Payment payment = paymentRepository.findBySandboxReference(req.sandboxReference())
                .orElseThrow(() -> new NotFoundException("Payment not found for sandboxReference"));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return PaymentMapper.toResponse(payment);
        }

        payment.setPaymentStatus(req.status());

        if (req.status() == PaymentStatus.SUCCESS && payment.getTransactionType() == TransactionType.TOP_UP) {
            User user = payment.getUser();
            user.setBalance(user.getBalance().add(payment.getAmount()));
        }

        Payment saved = paymentRepository.save(payment);
        return PaymentMapper.toResponse(saved);
    }

    private PaymentResponse createInternal(PaymentCreateRequest req) {
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Booking booking = null;
        if (req.bookingId() != null) {
            booking = bookingRepository.findById(req.bookingId())
                    .orElseThrow(() -> new NotFoundException("Booking not found"));
        }

        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than 0");
        }

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setBooking(booking);
        payment.setAmount(req.amount());
        payment.setProvider(req.provider());
        payment.setSandboxReference(req.sandboxReference());
        payment.setPaymentMethod(req.paymentMethod());
        payment.setTransactionType(req.transactionType());

        if (req.paymentMethod() == PaymentMethod.APP_CREDIT) {
            applyWalletRules(user, req.transactionType(), req.amount());
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setPaymentStatus(PaymentStatus.PENDING);

            if (payment.getSandboxReference() == null || payment.getSandboxReference().isBlank()) {
                payment.setSandboxReference("sandbox_" + UUID.randomUUID());
            }
            if (payment.getProvider() == null || payment.getProvider().isBlank()) {
                payment.setProvider("sandbox");
            }
        }

        Payment saved = paymentRepository.save(payment);
        return PaymentMapper.toResponse(saved);
    }

    private void applyWalletRules(User user, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.TOP_UP) {
            user.setBalance(user.getBalance().add(amount));
            return;
        }

        if (user.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient balance");
        }
        user.setBalance(user.getBalance().subtract(amount));
    }

    private void applyCheckoutSessionCompleted(Session session) {
        String sessionId = session.getId();
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Stripe checkout.session.completed missing session id");
            return;
        }

        Optional<Payment> paymentOptional = paymentRepository.findBySandboxReference(sessionId);
        if (paymentOptional.isEmpty()) {
            log.warn("Stripe webhook session not mapped to payment: {}", sessionId);
            return;
        }

        Payment payment = paymentOptional.get();
        if (isTerminalStatus(payment.getPaymentStatus())) {
            return;
        }

        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        if (payment.getTransactionType() == TransactionType.TOP_UP) {
            User user = payment.getUser();
            user.setBalance(user.getBalance().add(payment.getAmount()));
        }
        paymentRepository.save(payment);
    }

    private void markAsFailedIfPending(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Stripe failed event missing session id");
            return;
        }

        Optional<Payment> paymentOptional = paymentRepository.findBySandboxReference(sessionId);
        if (paymentOptional.isEmpty()) {
            log.warn("Stripe webhook session not mapped to payment: {}", sessionId);
            return;
        }

        Payment payment = paymentOptional.get();
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            return;
        }

        payment.setPaymentStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
    }

    private void markFromFailedPaymentIntent(PaymentIntent paymentIntent) {
        if (paymentIntent.getMetadata() == null) {
            log.warn("payment_intent.payment_failed without metadata");
            return;
        }

        String paymentIdRaw = paymentIntent.getMetadata().get("paymentId");
        if (paymentIdRaw != null && !paymentIdRaw.isBlank()) {
            try {
                long paymentId = Long.parseLong(paymentIdRaw);
                paymentRepository.findById(paymentId).ifPresent(payment -> {
                    if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                        payment.setPaymentStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                    }
                });
                return;
            } catch (NumberFormatException ex) {
                log.warn("Invalid paymentId metadata in payment_intent.payment_failed: {}", paymentIdRaw);
            }
        }

        String checkoutSessionId = paymentIntent.getMetadata().get("checkoutSessionId");
        if (checkoutSessionId != null && !checkoutSessionId.isBlank()) {
            markAsFailedIfPending(checkoutSessionId);
            return;
        }

        log.warn("payment_intent.payment_failed could not be mapped to local payment");
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "eur";
        }
        return currency.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isTerminalStatus(PaymentStatus status) {
        return status == PaymentStatus.SUCCESS
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.CANCELLED;
    }

    private <T extends StripeObject> T deserializeEventObject(Event event, Class<T> expectedType) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;
        Optional<StripeObject> stripeObjectOptional = dataObjectDeserializer.getObject();
        if (stripeObjectOptional.isPresent()) {
            stripeObject = stripeObjectOptional.get();
        } else {
            String eventApiVersion = normalizeApiVersion(event.getApiVersion());
            String sdkApiVersion = normalizeApiVersion(Stripe.API_VERSION);
            if (!isApiVersionMismatch(eventApiVersion, sdkApiVersion)) {
                log.warn(
                        "Safe Stripe event deserialization failed for event {} but no API version mismatch detected " +
                                "(eventApiVersion={}, sdkApiVersion={}). Unsafe fallback skipped.",
                        event.getType(),
                        eventApiVersion,
                        sdkApiVersion
                );
                return null;
            }
            log.warn(
                    "Safe Stripe event deserialization failed for event {} and API version mismatch was detected " +
                            "(eventApiVersion={}, sdkApiVersion={}). Trying unsafe fallback.",
                    event.getType(),
                    eventApiVersion,
                    sdkApiVersion
            );
            try {
                stripeObject = dataObjectDeserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException ex) {
                log.warn(
                        "Unsafe Stripe event deserialization failed for event {}: {}",
                        event.getType(),
                        ex.getMessage()
                );
                return null;
            } catch (RuntimeException ex) {
                log.warn(
                        "Unexpected error during unsafe Stripe event deserialization for event {}: {}",
                        event.getType(),
                        ex.getMessage()
                );
                return null;
            }
        }

        if (!expectedType.isInstance(stripeObject)) {
            log.warn("Unexpected Stripe event object type for {}: {}", event.getType(), stripeObject.getClass().getName());
            return null;
        }

        return expectedType.cast(stripeObject);
    }

    private boolean isApiVersionMismatch(String eventApiVersion, String sdkApiVersion) {
        if (eventApiVersion == null || sdkApiVersion == null) {
            return false;
        }
        return !eventApiVersion.equalsIgnoreCase(sdkApiVersion);
    }

    private String normalizeApiVersion(String apiVersion) {
        if (apiVersion == null) {
            return null;
        }
        String normalized = apiVersion.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
