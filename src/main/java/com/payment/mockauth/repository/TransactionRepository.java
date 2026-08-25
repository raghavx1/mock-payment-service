package com.payment.mockauth.repository;

import com.payment.mockauth.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, UUID> {
    Optional<TransactionRecord> findByIdempotencyKey(String idempotencyKey);
}