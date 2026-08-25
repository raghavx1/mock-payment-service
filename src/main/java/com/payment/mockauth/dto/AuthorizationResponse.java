package com.payment.mockauth.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AuthorizationResponse(
    String transactionId,
    String idempotencyKey,
    String status,
    String authorizationCode,
    String declineReason,
    BigDecimal amount,
    String currency,
    LocalDateTime timestamp,
    boolean fromCache
) implements Serializable {}