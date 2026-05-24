package com.example.bookingapp.ModuleSubscription.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeRequest {
    private Long userId;
    private Long planId;
    private String paymentMethod;
    private String transactionId;
}