package com.example.bookingapp.ModulePayment.service;

import com.example.bookingapp.ModulePayment.entity.PaymentEntity;
import com.example.bookingapp.ModulePayment.repository.PaymentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository PaymentRepository;

    // List of all payments
    public Page<PaymentEntity> fetchPaymentList(Pageable pageable) {
        return PaymentRepository.findAll(pageable);
    }

    // Find payments by user ID
    public Page<PaymentEntity> findByUserId(Long userId, Pageable pageable) {
        return PaymentRepository.findByUserId(userId, pageable);
    }

    // Save Payment
    public PaymentEntity savePayment(PaymentEntity payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }
        
        return PaymentRepository.save(payment);
    }
}
