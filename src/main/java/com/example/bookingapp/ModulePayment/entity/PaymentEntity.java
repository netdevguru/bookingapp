package com.example.bookingapp.ModulePayment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Column(nullable = false)
    private Long userId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "plan_id")
    private Long planId;

    @NotBlank(message = "Payment gateway is required")
    @Column(nullable = false)
    private String paymentGateway; // RAZORPAY or STRIPE

    @NotBlank(message = "Transaction ID is required")
    @Column(nullable = false, unique = true)
    private String transactionId; // Payment ID from gateway

    @Column(name = "order_id")
    private String orderId; // Order ID (Razorpay) or Session ID (Stripe)

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Column(nullable = false)
    private String currency;

    @NotBlank(message = "Payment status is required")
    @Column(nullable = false)
    private String status; // SUCCESS, FAILED, PENDING

    @Column(name = "payment_method")
    private String paymentMethod; // card, upi, netbanking, etc.

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "plan_description", length = 500)
    private String planDescription;

    @Column(name = "signature")
    private String signature; // For Razorpay signature verification

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
