package com.unir.bikeshare.backend.payments.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Busca un pago por su id.
     *
     * @param id id del pago
     * @return DTO de respuesta del pago
     * @throws NotFoundException si no existe el pago
     */
    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        return PaymentMapper.toResponse(payment);
    }

    /**
     * Devuelve todos los pagos.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll()
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }

    /**
     * Devuelve todos los pagos de un usuario.
     *
     * @param userId id del usuario
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getByUser(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }

    /**
     * Devuelve todos los pagos asociados a una booking.
     *
     * @param bookingId id de la booking
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }
    
    //CREATE

    /**
     * Crea un pago.
     *
     * <p><b>Casos principales</b></p>
     * <ul>
     *   <li><b>APP_CREDIT</b>:
     *      <ul>
     *        <li>Si es TOP_UP → suma saldo.</li>
     *        <li>Si es RENTAL_PAYMENT → valida si el saldo es suficiente y resta.</li>
     *        <li>El pago se marca como SUCCESS inmediatamente.</li>
     *      </ul>
     *   </li>
     *   <li><b>Método externo (CREDIT_CARD / PAYPAL)</b>:
     *      <ul>
     *        <li>Se crea el pago en estado PENDING (como "intent").</li>
     *        <li>No se toca el balance del usuario aquí (espera confirmación).</li>
     *        <li>Se asegura un sandboxReference y provider para poder "identificarlo" cuando llegue el webhook.</li>
     *      </ul>
     *   </li>
     * </ul>
     *
     * @param req petición de creación del pago
     * @return PaymentResponse con el pago creado
     * @throws NotFoundException si userId o bookingId no existen
     * @throws BusinessException si amount <= 0 o si no hay saldo suficiente (APP_CREDIT + RENTAL_PAYMENT)
     */
    public PaymentResponse create(PaymentCreateRequest req, Long requesterUserId, boolean requesterAdmin) {
        if (!requesterAdmin && !Objects.equals(req.userId(), requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create payments for yourself");
        }
        return createInternal(req);
    }

    private PaymentResponse createInternal(PaymentCreateRequest req) {

    	//comprueba usuario y lo asigna
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        //comprueba que existe el booking y lo asigna
        Booking booking = null;
        if (req.bookingId() != null) {
            booking = bookingRepository.findById(req.bookingId())
                    .orElseThrow(() -> new NotFoundException("Booking not found"));
        }

        // Validación extra por seguridad de la cantidad
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than 0");
        }

        // Crear Payment entity
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setBooking(booking);
        payment.setAmount(req.amount());
        payment.setProvider(req.provider());
        payment.setSandboxReference(req.sandboxReference());
        payment.setPaymentMethod(req.paymentMethod());
        payment.setTransactionType(req.transactionType());
        
        if (req.paymentMethod() == PaymentMethod.APP_CREDIT) {
            // Wallet interno: se cobra ya
            applyWalletRules(user, req.transactionType(), req.amount());
            payment.setPaymentStatus(com.unir.bikeshare.backend.payments.model.PaymentStatus.SUCCESS);
        } else {
            // Externo: se crea intent pendiente
            payment.setPaymentStatus(com.unir.bikeshare.backend.payments.model.PaymentStatus.PENDING);

            // Opcional: si no hay sandboxReference
            if (payment.getSandboxReference() == null || payment.getSandboxReference().isBlank()) {
                payment.setSandboxReference("sandbox_" + java.util.UUID.randomUUID());
            }
            if (payment.getProvider() == null || payment.getProvider().isBlank()) {
                payment.setProvider("sandbox");
            }
        }

        Payment saved = paymentRepository.save(payment);
        return PaymentMapper.toResponse(saved);
    }
    
    // REGLAS DE WALLET

    /**
     * Aplica las reglas de negocio del "wallet" (saldo interno) sobre un usuario.
     *
     * <p>Se llama SOLO cuando el método es APP_CREDIT (credito de la app).</p>
     *
     * <ul>
     *   <li>TOP_UP: suma el amount al balance.</li>
     *   <li>RENTAL_PAYMENT: comprueba saldo suficiente y resta el amount.</li>
     * </ul>
     *
     * @param user usuario al que se le modifica el balance
     * @param type tipo de transacción (TOP_UP o RENTAL_PAYMENT)
     * @param amount importe
     * @throws BusinessException si el saldo es insuficiente en RENTAL_PAYMENT
     */
    private void applyWalletRules(User user, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.TOP_UP) { //si aumenta el saldo dentro de la app
            user.setBalance(user.getBalance().add(amount));
            return;
        }
        // RENTAL_PAYMENT
        if (user.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient balance");
        }
        user.setBalance(user.getBalance().subtract(amount));
    }
    
    //webhook de mentira
    
    /**
     * "Webhook de mentira" para simular el callback de una pasarela de pago externa.
     *
     * <p>En integraciones reales (Stripe/PayPal), este endpoint lo llama la pasarela cuando un pago cambia de estado:
     * SUCCESS / FAILED / CANCELLED.</p>
     *
     * <p><b>Por qué existe</b></p>
     * <ul>
     *   <li>Permite modelar el flujo real: Payment PENDING → confirmación → SUCCESS/FAILED.</li>
     *   <li>Evita asumir que un pago externo siempre tiene éxito inmediatamente.</li>
     *   <li>Hace el backend compatible con integraciones futuras sin reescribir el dominio.</li>
     * </ul>
     *
     * <p><b>Idempotencia básica</b></p>
     * <ul>
     *   <li>Si el pago ya está SUCCESS, no se vuelve a aplicar nada (evita duplicados).</li>
     * </ul>
     *
     * <p><b>Efecto sobre el balance</b></p>
     * <ul>
     *   <li>Si el pago es externo y se confirma SUCCESS:</li>
     *   <li>TOP_UP: ahora sí sumamos balance (porque ya se ha pagado de verdad).</li>
     *   <li>RENTAL_PAYMENT: NO tocamos balance (el dinero vino de fuera, no de la wallet).</li>
     * </ul>
     *
     * @param req request con sandboxReference + nuevo status
     * @return PaymentResponse actualizado
     * @throws NotFoundException si no existe ningún pago con ese sandboxReference
     */
    public PaymentResponse confirmWebhook(com.unir.bikeshare.backend.payments.dto.PaymentWebhookRequest req) {

        Payment payment = paymentRepository.findBySandboxReference(req.sandboxReference())
                .orElseThrow(() -> new NotFoundException("Payment not found for sandboxReference"));

        // Si ya está SUCCESS, no hacemos nada (idempotencia básica)
        if (payment.getPaymentStatus() == com.unir.bikeshare.backend.payments.model.PaymentStatus.SUCCESS) {
            return PaymentMapper.toResponse(payment);
        }

        payment.setPaymentStatus(req.status());

        // Si el pago externo se confirma como SUCCESS, aplicamos efecto interno solo si procede
        if (req.status() == com.unir.bikeshare.backend.payments.model.PaymentStatus.SUCCESS) {
            // TOP_UP externo -> suma balance ahora (porque ahora sí “se pagó”)
            if (payment.getTransactionType() == TransactionType.TOP_UP) {
                User user = payment.getUser();
                user.setBalance(user.getBalance().add(payment.getAmount()));
            }
            // RENTAL_PAYMENT externo -> no toca balance (dinero viene de fuera)
        }

        Payment saved = paymentRepository.save(payment);
        return PaymentMapper.toResponse(saved);
    }

}
