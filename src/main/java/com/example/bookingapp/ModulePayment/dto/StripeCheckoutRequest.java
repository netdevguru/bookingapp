package com.example.bookingapp.ModulePayment.dto;

import lombok.Data;

@Data
public class StripeCheckoutRequest {
    private Long planId;
    private Long userId;
    private String successUrl;
    private String cancelUrl;
}
