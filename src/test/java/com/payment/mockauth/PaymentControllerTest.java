package com.payment.mockauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.mockauth.controller.PaymentController;
import com.payment.mockauth.dto.AuthorizationRequest;
import com.payment.mockauth.dto.AuthorizationResponse;
import com.payment.mockauth.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAuthorizePaymentSuccessfully() throws Exception {
        String key = UUID.randomUUID().toString();
        AuthorizationRequest request = new AuthorizationRequest("ACC-123", new BigDecimal("150.00"), "USD", "4111222233334444");

        AuthorizationResponse mockResponse = new AuthorizationResponse(
            UUID.randomUUID().toString(), key, "AUTHORIZED", "AUTH-987654", null, new BigDecimal("150.00"), "USD", LocalDateTime.now(), false
        );

        Mockito.when(paymentService.processAuthorization(eq(key), any(AuthorizationRequest.class)))
               .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/payments/authorize")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.authorizationCode").value("AUTH-987654"))
                .andExpect(jsonPath("$.idempotencyKey").value(key));
    }

    @Test
    void shouldFailWhenIdempotencyKeyIsMissing() throws Exception {
        AuthorizationRequest request = new AuthorizationRequest("ACC-123", new BigDecimal("150.00"), "USD", "4111222233334444");

        mockMvc.perform(post("/api/v1/payments/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}