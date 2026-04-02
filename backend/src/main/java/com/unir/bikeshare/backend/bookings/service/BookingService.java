package com.unir.bikeshare.backend.bookings.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unir.bikeshare.backend.bikes.model.Bike;
import com.unir.bikeshare.backend.bikes.model.BikeStatus;
import com.unir.bikeshare.backend.bikes.repository.BikeRepository;
import com.unir.bikeshare.backend.bookings.dto.BookingCreatePaymentResponse;
import com.unir.bikeshare.backend.bookings.dto.BookingCreateRequest;
import com.unir.bikeshare.backend.bookings.dto.BookingResponse;
import com.unir.bikeshare.backend.bookings.dto.BookingUnlockPaymentMethod;
import com.unir.bikeshare.backend.bookings.dto.BookingUpdateRequest;
import com.unir.bikeshare.backend.bookings.mapper.BookingMapper;
import com.unir.bikeshare.backend.bookings.model.Booking;
import com.unir.bikeshare.backend.bookings.model.BookingStatus;
import com.unir.bikeshare.backend.bookings.repository.BookingRepository;
import com.unir.bikeshare.backend.common.exception.BusinessException;
import com.unir.bikeshare.backend.common.exception.NotFoundException;
import com.unir.bikeshare.backend.payments.config.RentalPaymentProperties;
import com.unir.bikeshare.backend.payments.config.StripeProperties;
import com.unir.bikeshare.backend.payments.dto.StripeCheckoutSessionResponse;
import com.unir.bikeshare.backend.payments.model.Payment;
import com.unir.bikeshare.backend.payments.model.PaymentMethod;
import com.unir.bikeshare.backend.payments.model.PaymentStatus;
import com.unir.bikeshare.backend.payments.model.TransactionType;
import com.unir.bikeshare.backend.payments.repository.PaymentRepository;
import com.unir.bikeshare.backend.payments.stripe.StripeCheckoutSessionData;
import com.unir.bikeshare.backend.payments.stripe.StripeGateway;
import com.unir.bikeshare.backend.stations.model.Station;
import com.unir.bikeshare.backend.stations.repository.StationRepository;
import com.unir.bikeshare.backend.users.model.User;
import com.unir.bikeshare.backend.users.repository.UserRepository;

@Service
@Transactional
public class BookingService {
    private static final long PENDING_RESERVATION_WINDOW_SECONDS = 15 * 60;

    private static final List<BookingStatus> ACTIVE_OR_PENDING = List.of(
            BookingStatus.PENDING,
            BookingStatus.ACTIVE
    );

    private static final List<BikeStatus> DOCK_OCCUPYING_STATUSES = List.of(
            BikeStatus.AVAILABLE,
            BikeStatus.BOOKED,
            BikeStatus.MAINTENANCE
    );

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BikeRepository bikeRepository;
    private final StationRepository stationRepository;
    private final PaymentRepository paymentRepository;
    private final RentalPaymentProperties rentalPaymentProperties;
    private final StripeGateway stripeGateway;
    private final StripeProperties stripeProperties;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            BikeRepository bikeRepository,
            StationRepository stationRepository,
            PaymentRepository paymentRepository,
            RentalPaymentProperties rentalPaymentProperties,
            StripeGateway stripeGateway,
            StripeProperties stripeProperties
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.bikeRepository = bikeRepository;
        this.stationRepository = stationRepository;
        this.paymentRepository = paymentRepository;
        this.rentalPaymentProperties = rentalPaymentProperties;
        this.stripeGateway = stripeGateway;
        this.stripeProperties = stripeProperties;
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        return BookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAll() {
        return bookingRepository.findAll()
                .stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getByUser(Long userId) {
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getByBike(Long bikeId) {
        return bookingRepository.findByBikeId(bikeId)
                .stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    public BookingCreatePaymentResponse create(BookingCreateRequest req) {
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Bike bike = bikeRepository.findById(req.bikeId())
                .orElseThrow(() -> new NotFoundException("Bike not found"));

        validateUnlockPreconditions(user, bike);

        return switch (req.paymentMethod()) {
            case SALDO -> createWalletUnlock(req, user, bike);
            case STRIPE -> createStripeUnlockPending(req, user, bike);
        };
    }

    public BookingResponse update(Long id, BookingUpdateRequest req) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (req.expiryTime() != null) {
            booking.setExpiryTime(req.expiryTime());
        }

        if (req.status() != null) {
            applyStatusChange(booking, req.status());
        }

        Booking saved = bookingRepository.save(booking);
        return BookingMapper.toResponse(saved);
    }

    public BookingResponse returnBike(Long bookingId, Long stationId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BusinessException("Only ACTIVE bookings can be returned");
        }

        Station destinationStation = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found"));

        long occupiedDocks = bikeRepository.countByStationIdAndStatusIn(
                stationId,
                DOCK_OCCUPYING_STATUSES
        );

        if (occupiedDocks >= destinationStation.getCapacity()) {
            throw new BusinessException("Station is full");
        }

        Bike bike = booking.getBike();
        bike.setStation(destinationStation);
        bike.setStatus(BikeStatus.AVAILABLE);
        booking.setDropoffStation(destinationStation);
        booking.setReturnedAt(Instant.now());
        booking.setStatus(BookingStatus.COMPLETED);

        Booking saved = bookingRepository.save(booking);
        return BookingMapper.toResponse(saved);
    }

    private void validateUnlockPreconditions(User user, Bike bike) {
        if (bike.getStatus() != BikeStatus.AVAILABLE) {
            throw new BusinessException("Bike is not available");
        }

        boolean userHasActiveOrPending = bookingRepository.existsByUserIdAndStatusIn(user.getId(), ACTIVE_OR_PENDING);
        if (userHasActiveOrPending) {
            throw new BusinessException("User already has an active or pending booking");
        }

        boolean hasActiveOrPending = bookingRepository.existsByBikeIdAndStatusIn(bike.getId(), ACTIVE_OR_PENDING);
        if (hasActiveOrPending) {
            throw new BusinessException("Bike already has an active or pending booking");
        }
    }

    private BookingCreatePaymentResponse createWalletUnlock(BookingCreateRequest req, User user, Bike bike) {
        BigDecimal unlockFee = rentalPaymentProperties.getUnlockFee();
        if (unlockFee == null || unlockFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Unlock fee is not configured correctly");
        }

        if (user.getBalance().compareTo(unlockFee) < 0) {
            throw new BusinessException("Insufficient balance for unlock fee");
        }

        user.setBalance(user.getBalance().subtract(unlockFee));

        Booking booking = BookingMapper.toEntity(req, user, bike);
        booking.setPickupStation(bike.getStation());
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setActivatedAt(Instant.now());
        bike.setStatus(BikeStatus.BUSY);
        bike.setStation(null);
        Booking savedBooking = bookingRepository.save(booking);

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setBooking(savedBooking);
        payment.setAmount(unlockFee);
        payment.setProvider("wallet");
        payment.setPaymentMethod(PaymentMethod.APP_CREDIT);
        payment.setTransactionType(TransactionType.RENTAL_PAYMENT);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        return new BookingCreatePaymentResponse(
                BookingMapper.toResponse(savedBooking),
                BookingUnlockPaymentMethod.SALDO,
                PaymentStatus.SUCCESS,
                null
        );
    }

    private BookingCreatePaymentResponse createStripeUnlockPending(BookingCreateRequest req, User user, Bike bike) {
        BigDecimal unlockFee = rentalPaymentProperties.getUnlockFee();
        if (unlockFee == null || unlockFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Unlock fee is not configured correctly");
        }

        String successUrl = stripeProperties.getSuccessUrl();
        String cancelUrl = stripeProperties.getCancelUrl();
        if (successUrl == null || successUrl.isBlank() || cancelUrl == null || cancelUrl.isBlank()) {
            throw new BusinessException("Stripe success/cancel URLs are not configured");
        }

        String currency = normalizeCurrency(stripeProperties.getCurrency());

        Booking booking = BookingMapper.toEntity(req, user, bike);
        booking.setPickupStation(bike.getStation());
        booking.setExpiryTime(Instant.now().plusSeconds(PENDING_RESERVATION_WINDOW_SECONDS));
        bike.setStatus(BikeStatus.BOOKED);
        Booking savedBooking = bookingRepository.save(booking);

        StripeCheckoutSessionData sessionData = stripeGateway.createTopUpCheckoutSession(
                user.getId(),
                unlockFee,
                currency,
                successUrl,
                cancelUrl
        );

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setBooking(savedBooking);
        payment.setAmount(unlockFee);
        payment.setProvider("stripe");
        payment.setSandboxReference(sessionData.sessionId());
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setTransactionType(TransactionType.RENTAL_PAYMENT);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        Payment savedPayment = paymentRepository.save(payment);

        return new BookingCreatePaymentResponse(
                BookingMapper.toResponse(savedBooking),
                BookingUnlockPaymentMethod.STRIPE,
                savedPayment.getPaymentStatus(),
                new StripeCheckoutSessionResponse(
                        savedPayment.getId(),
                        sessionData.sessionId(),
                        sessionData.checkoutUrl(),
                        savedPayment.getPaymentStatus()
                )
        );
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "eur";
        }
        return currency.trim().toLowerCase();
    }

    private void applyStatusChange(Booking booking, BookingStatus newStatus) {
        BookingStatus current = booking.getStatus();
        Bike bike = booking.getBike();

        if (current == newStatus) {
            return;
        }

        switch (newStatus) {
            case ACTIVE -> {
                if (current != BookingStatus.PENDING) {
                    throw new BusinessException("Only PENDING bookings can be activated");
                }
                if (booking.getActivatedAt() == null) {
                    booking.setActivatedAt(Instant.now());
                }
                booking.setStatus(BookingStatus.ACTIVE);
                bike.setStatus(BikeStatus.BUSY);
                bike.setStation(null);
            }
            case COMPLETED -> throw new BusinessException("Use return endpoint to complete an ACTIVE booking");
            case CANCELLED -> {
                if (current != BookingStatus.PENDING) {
                    throw new BusinessException("Only PENDING bookings can be cancelled");
                }
                booking.setStatus(BookingStatus.CANCELLED);
                bike.setStatus(BikeStatus.AVAILABLE);
            }
            case PENDING -> throw new BusinessException("Cannot revert booking status to PENDING");
        }
    }
}
