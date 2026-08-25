package com.payment.mockauth.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AuthorizationRequest(
    @NotBlank(message = "AccountId is required")
    String accountId,

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be strictly positive")
    BigDecimal amount,

    @NotBlank(message = "Currency must be provided")
    @Size(min = 3, max = 3, message = "Currency must be ISO-3 (e.g. USD, INR)")
    String currency,

    @NotBlank(message = "Card number is required")
    @Size(min = 13, max = 19, message = "Invalid card number length")
    String cardNumber
) {}