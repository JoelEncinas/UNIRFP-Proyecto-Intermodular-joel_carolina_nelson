package com.unir.bikeshare.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unir.bikeshare.backend.bikes.model.Bike;
import com.unir.bikeshare.backend.bikes.model.BikeStatus;
import com.unir.bikeshare.backend.bikes.repository.BikeRepository;
import com.unir.bikeshare.backend.bookings.model.Booking;
import com.unir.bikeshare.backend.bookings.model.BookingStatus;
import com.unir.bikeshare.backend.bookings.repository.BookingRepository;
import com.unir.bikeshare.backend.payments.model.Payment;
import com.unir.bikeshare.backend.payments.model.PaymentMethod;
import com.unir.bikeshare.backend.payments.model.PaymentStatus;
import com.unir.bikeshare.backend.payments.repository.PaymentRepository;
import com.unir.bikeshare.backend.payments.stripe.StripeGateway;
import com.unir.bikeshare.backend.users.model.User;
import com.unir.bikeshare.backend.users.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/sql/reset-test-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
class BookingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private StripeGateway stripeGateway;

    @BeforeEach
    void setUpStripeMock() {
        when(stripeGateway.refundCheckoutSessionPayment(anyString())).thenReturn(true);
    }

    @Test
    void walletFlowCreatesActiveBookingAndCompletesReturnWithStateTransitions() throws Exception {
        String token = loginToken("rider1");

        String createPayload = objectMapper.writeValueAsString(Map.of(
                "userId", 2,
                "bikeId", 1,
                "paymentMethod", "SALDO"
        ));

        MvcResult createResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.booking.status").value("ACTIVE"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long bookingId = responseJson.path("booking").path("id").asLong();

        Booking createdBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(createdBooking.getStatus()).isEqualTo(BookingStatus.ACTIVE);
        assertThat(createdBooking.getActivatedAt()).isNotNull();
        assertThat(createdBooking.getPickupStation()).isNotNull();
        assertThat(createdBooking.getPickupStation().getId()).isEqualTo(2L);

        Bike bookedBike = bikeRepository.findById(1L).orElseThrow();
        assertThat(bookedBike.getStatus()).isEqualTo(BikeStatus.BUSY);
        assertThat(bookedBike.getStation()).isNull();

        User rider = userRepository.findById(2L).orElseThrow();
        assertThat(rider.getBalance()).isEqualByComparingTo(new BigDecimal("9.00"));

        List<Payment> createdPayments = paymentRepository.findByBookingId(bookingId);
        assertThat(createdPayments).hasSize(1);
        Payment payment = createdPayments.getFirst();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.APP_CREDIT);
        assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("1.00"));

        String returnPayload = objectMapper.writeValueAsString(Map.of("stationId", 2));
        mockMvc.perform(post("/api/bookings/" + bookingId + "/return")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.dropoffStationId").value(2));

        Booking returnedBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(returnedBooking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(returnedBooking.getReturnedAt()).isNotNull();
        assertThat(returnedBooking.getDropoffStation()).isNotNull();
        assertThat(returnedBooking.getDropoffStation().getId()).isEqualTo(2L);

        Bike returnedBike = bikeRepository.findById(1L).orElseThrow();
        assertThat(returnedBike.getStatus()).isEqualTo(BikeStatus.AVAILABLE);
        assertThat(returnedBike.getStation()).isNotNull();
        assertThat(returnedBike.getStation().getId()).isEqualTo(2L);
    }

    @Test
    void createBookingRejectsWhenWalletBalanceIsInsufficient() throws Exception {
        String token = loginToken("rider2");
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", 3,
                "bikeId", 6,
                "paymentMethod", "SALDO"
        ));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient balance for unlock fee"));
    }

    @Test
    void createBookingRejectsWhenBikeIsNotAvailable() throws Exception {
        String token = loginToken("rider1");
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", 2,
                "bikeId", 2,
                "paymentMethod", "SALDO"
        ));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bike is not available"));
    }

    @Test
    void returnBookingRejectsWhenDestinationStationIsFull() throws Exception {
        String token = loginToken("rider_active");
        String payload = objectMapper.writeValueAsString(Map.of("stationId", 1));

        mockMvc.perform(post("/api/bookings/1/return")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Station is full"));

        Booking booking = bookingRepository.findById(1L).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACTIVE);

        Bike bike = bikeRepository.findById(7L).orElseThrow();
        assertThat(bike.getStatus()).isEqualTo(BikeStatus.BUSY);
        assertThat(bike.getStation()).isNull();
    }

    @Test
    void createBookingRejectsWhenUserAlreadyHasActiveOrPendingBooking() throws Exception {
        String token = loginToken("rider_active");
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", 4,
                "bikeId", 6,
                "paymentMethod", "SALDO"
        ));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User already has an active or pending booking"));
    }

    private String loginToken(String username) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", "asdasd"
        ));

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }
}
