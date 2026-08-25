package com.payment.mockauth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.mockauth.dto.AuthorizationRequest;
import com.payment.mockauth.dto.AuthorizationResponse;
import com.payment.mockauth.model.TransactionRecord;
import com.payment.mockauth.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Random;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String IDEMPOTENCY_PREFIX = "idemp:";

    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, String> redisTemplate; // Changed to String, String
    private final ObjectMapper objectMapper;

    @Value("${payment.idempotency-ttl-seconds:86400}")
    private long idempotencyTtl;

    public PaymentService(TransactionRepository transactionRepository, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuthorizationResponse processAuthorization(String idempotencyKey, AuthorizationRequest request) {
        String cacheKey = IDEMPOTENCY_PREFIX + idempotencyKey;

        // 1. Check Redis (Manual String parsing)
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                AuthorizationResponse cachedResponse = objectMapper.readValue(cachedJson, AuthorizationResponse.class);
                log.info("Idempotency hit in Redis for key: {}", idempotencyKey);
                
                return new AuthorizationResponse(
                    cachedResponse.transactionId(),
                    cachedResponse.idempotencyKey(),
                    cachedResponse.status(),
                    cachedResponse.authorizationCode(),
                    cachedResponse.declineReason(),
                    cachedResponse.amount(),
                    cachedResponse.currency(),
                    cachedResponse.timestamp(),
                    true // served from cache
                );
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed, falling back to database check: {}", e.getMessage());
        }

        // 2. Fallback DB check
        var existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent()) {
            log.info("Idempotency hit in DB for key: {}", idempotencyKey);
            return mapToResponse(existingTx.get(), false);
        }

        // 3. Process new request
        String status = "AUTHORIZED";
        String authCode = "AUTH-" + (100000 + new Random().nextInt(900000));
        String declineReason = null;

        if (request.cardNumber().endsWith("0000")) {
            status = "DECLINED";
            authCode = null;
            declineReason = "INSUFFICIENT_FUNDS";
        }

        TransactionRecord saved = transactionRepository.save(new TransactionRecord(
            idempotencyKey, request.accountId(), request.amount(), request.currency(), status, authCode, declineReason
        ));
        
        AuthorizationResponse response = mapToResponse(saved, false);

        // 4. Save to Redis as raw JSON String
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, responseJson, Duration.ofSeconds(idempotencyTtl));
        } catch (Exception e) {
            log.error("Failed to write to Redis cache", e);
        }

        return response;
    }

    private AuthorizationResponse mapToResponse(TransactionRecord tx, boolean fromCache) {
        return new AuthorizationResponse(
            tx.getId().toString(), tx.getIdempotencyKey(), tx.getStatus(),
            tx.getAuthorizationCode(), tx.getDeclineReason(), tx.getAmount(),
            tx.getCurrency(), tx.getCreatedAt(), fromCache
        );
    }
}