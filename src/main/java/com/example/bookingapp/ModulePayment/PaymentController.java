package com.example.bookingapp.ModulePayment;

import com.example.bookingapp.ModulePayment.dto.RazorpayPaymentRequest;
import com.example.bookingapp.ModulePayment.dto.StripePaymentRequest;
import com.example.bookingapp.ModulePayment.service.RazorpayService;
import com.example.bookingapp.ModulePayment.service.StripeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/payment")
@Validated
public class PaymentController {

    @Autowired
    private PaymentService PaymentService;

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private StripeService stripeService;

    @PostMapping
    public ResponseEntity<PaymentEntity> createPayment(@Valid @RequestBody PaymentEntity Payment) {
        PaymentEntity savedPayment = PaymentService.savePayment(Payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPayment);
    }

    @PostMapping("/razorpay/complete")
    public ResponseEntity<Map<String, Object>> completeRazorpayPayment(
            @Valid @RequestBody RazorpayPaymentRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Verify payment signature
            boolean isValid = razorpayService.verifyPaymentSignature(
                request.getOrderId(), 
                request.getPaymentId(), 
                request.getSignature()
            );
            
            if (!isValid) {
                response.put("status", "error");
                response.put("message", "Invalid payment signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Create payment record
            PaymentEntity payment = PaymentEntity.builder()
                .userId(request.getUserId())
                .planId(request.getPlanId())
                .paymentGateway("RAZORPAY")
                .transactionId(request.getPaymentId())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("SUCCESS")
                .paymentMethod(request.getPaymentMethod())
                .planName(request.getPlanName())
                .planDescription(request.getPlanDescription())
                .signature(request.getSignature())
                .paymentDate(LocalDateTime.now())
                .build();
            
            PaymentEntity savedPayment = PaymentService.savePayment(payment);
            
            response.put("status", "success");
            response.put("message", "Payment completed successfully");
            response.put("payment", savedPayment);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Payment processing failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/stripe/complete")
    public ResponseEntity<Map<String, Object>> completeStripePayment(
            @Valid @RequestBody StripePaymentRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Verify session with Stripe (optional but recommended)
            stripeService.getSessionMetadata(request.getSessionId());
            
            // Create payment record
            PaymentEntity payment = PaymentEntity.builder()
                .userId(request.getUserId())
                .planId(request.getPlanId())
                .paymentGateway("STRIPE")
                .transactionId(request.getPaymentIntentId())
                .orderId(request.getSessionId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("SUCCESS")
                .paymentMethod(request.getPaymentMethod())
                .planName(request.getPlanName())
                .planDescription(request.getPlanDescription())
                .paymentDate(LocalDateTime.now())
                .build();
            
            PaymentEntity savedPayment = PaymentService.savePayment(payment);
            
            response.put("status", "success");
            response.put("message", "Payment completed successfully");
            response.put("payment", savedPayment);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Payment processing failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/failed")
    public ResponseEntity<Map<String, Object>> recordFailedPayment(
            @RequestBody Map<String, Object> failureData) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            PaymentEntity payment = PaymentEntity.builder()
                .userId(((Number) failureData.get("userId")).longValue())
                .planId(failureData.containsKey("planId") ? 
                    ((Number) failureData.get("planId")).longValue() : null)
                .paymentGateway((String) failureData.get("paymentGateway"))
                .transactionId((String) failureData.get("transactionId"))
                .orderId((String) failureData.get("orderId"))
                .amount(new BigDecimal(failureData.get("amount").toString()))
                .currency((String) failureData.get("currency"))
                .status("FAILED")
                .errorMessage((String) failureData.get("errorMessage"))
                .paymentDate(LocalDateTime.now())
                .build();
            
            PaymentEntity savedPayment = PaymentService.savePayment(payment);
            
            response.put("status", "recorded");
            response.put("message", "Failed payment recorded");
            response.put("payment", savedPayment);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to record payment failure: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllPayments(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        
        org.springframework.data.domain.Sort.Direction direction = 
            sortDirection.equalsIgnoreCase("asc") ? 
            org.springframework.data.domain.Sort.Direction.ASC : 
            org.springframework.data.domain.Sort.Direction.DESC;
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, sortBy));
        
        org.springframework.data.domain.Page<PaymentEntity> paymentPage = PaymentService.fetchPaymentList(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("payments", paymentPage.getContent());
        response.put("currentPage", paymentPage.getNumber());
        response.put("totalPages", paymentPage.getTotalPages());
        response.put("totalItems", paymentPage.getTotalElements());
        response.put("pageSize", paymentPage.getSize());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPaymentsByUser(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        
        org.springframework.data.domain.Sort.Direction direction = 
            sortDirection.equalsIgnoreCase("asc") ? 
            org.springframework.data.domain.Sort.Direction.ASC : 
            org.springframework.data.domain.Sort.Direction.DESC;
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, sortBy));
        
        org.springframework.data.domain.Page<PaymentEntity> paymentPage = 
            PaymentService.findByUserId(userId, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("payments", paymentPage.getContent());
        response.put("currentPage", paymentPage.getNumber());
        response.put("totalPages", paymentPage.getTotalPages());
        response.put("totalItems", paymentPage.getTotalElements());
        response.put("pageSize", paymentPage.getSize());
        
        return ResponseEntity.ok(response);
    }
}
