package com.example.bookingapp.ModulePayment.repository;

import com.example.bookingapp.ModulePayment.entity.PaymentEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    
    // Find payments by user ID with pagination
    Page<PaymentEntity> findByUserId(Long userId, Pageable pageable);
    
    // Find all payments by user ID
    List<PaymentEntity> findByUserId(Long userId);
    
    // Find payment by transaction ID
    Optional<PaymentEntity> findByTransactionId(String transactionId);
    
    // Find payments by payment gateway
    Page<PaymentEntity> findByPaymentGateway(String paymentGateway, Pageable pageable);
    
    // Find payments by status
    Page<PaymentEntity> findByStatus(String status, Pageable pageable);
    
    // Find payments by user ID and status
    Page<PaymentEntity> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
}
