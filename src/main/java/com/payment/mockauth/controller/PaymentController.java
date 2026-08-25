package com.payment.mockauth.controller;

import com.payment.mockauth.dto.AuthorizationRequest;
import com.payment.mockauth.dto.AuthorizationResponse;
import com.payment.mockauth.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizationResponse> authorize(
        @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
        @Valid @RequestBody AuthorizationRequest request
    ) {
        if (idempotencyKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        AuthorizationResponse response = paymentService.processAuthorization(idempotencyKey, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}