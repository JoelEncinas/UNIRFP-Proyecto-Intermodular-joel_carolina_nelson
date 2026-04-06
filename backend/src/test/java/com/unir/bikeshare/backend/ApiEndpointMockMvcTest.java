package com.unir.bikeshare.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/sql/reset-test-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
class ApiEndpointMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerReturnsTokenWhenPayloadIsValid() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "username", "new_rider",
                "email", "new_rider@test.com",
                "password", "asdasd"
        ));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registerReturnsConflictWhenUsernameAlreadyExists() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "username", "rider1",
                "email", "different_email@test.com",
                "password", "asdasd"
        ));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "username", "different_user",
                "email", "rider1@test.com",
                "password", "asdasd"
        ));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "username", "rider1",
                "password", "asdasd"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "username", "rider1",
                "password", "bad_password"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void stationsAndAvailableBikesEndpointsReturnExpectedData() throws Exception {
        String token = loginToken("rider1");

        mockMvc.perform(get("/api/stations")
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber());

        mockMvc.perform(get("/api/bikes")
                        .param("status", "AVAILABLE")
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("AVAILABLE"))));
    }

    @Test
    void usersMeRequiresAuthenticationAndReturnsCurrentUserWhenAuthenticated() throws Exception {
        MvcResult unauthenticatedResult = mockMvc.perform(get("/api/users/me"))
                .andReturn();
        assertThat(unauthenticatedResult.getResponse().getStatus()).isIn(401, 403);

        String token = loginToken("rider1");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("rider1"));
    }

    @Test
    void bookingsEndpointReturnsOnlyOwnBookingsForRider() throws Exception {
        String riderToken = loginToken("rider1");

        mockMvc.perform(get("/api/bookings")
                        .header("Authorization", bearerToken(riderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", everyItem(is(2))));
    }

    @Test
    void bookingsEndpointRejectsRiderWhenFilteringAnotherUser() throws Exception {
        String riderToken = loginToken("rider1");

        mockMvc.perform(get("/api/bookings")
                        .param("userId", "6")
                        .header("Authorization", bearerToken(riderToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void bookingsEndpointAllowsAdminFiltersByUserAndBike() throws Exception {
        String adminToken = loginToken("admin1");

        mockMvc.perform(get("/api/bookings")
                        .param("userId", "6")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(6))
                .andExpect(jsonPath("$[0].id").value(4));

        mockMvc.perform(get("/api/bookings")
                        .param("bikeId", "8")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bikeId").value(8))
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void createBookingReturnsValidationErrorWhenPaymentMethodIsMissing() throws Exception {
        String token = loginToken("rider1");
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", 2,
                "bikeId", 1
        ));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    void createBookingRejectsNonAvailableBike() throws Exception {
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
    void returnEndpointRejectsPendingBooking() throws Exception {
        String token = loginToken("rider_pending");
        String payload = objectMapper.writeValueAsString(Map.of("stationId", 2));

        mockMvc.perform(post("/api/bookings/2/return")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only ACTIVE bookings can be returned"));
    }

    @Test
    void paymentConfigEndpointReturnsExpectedFields() throws Exception {
        String token = loginToken("rider1");

        mockMvc.perform(get("/api/payments/config")
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlockFee").value(1))
                .andExpect(jsonPath("$.currency").value("eur"))
                .andExpect(jsonPath("$.minTopUpAmount").value(0.5));
    }

    private String loginToken(String username) throws Exception {
        String loginPayload = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", "asdasd"
        ));

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }
}
