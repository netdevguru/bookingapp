package com.example.bookingapp.ModulePayment.dto;

import lombok.Data;

@Data
public class RazorpayOrderRequest {
    private Double amount;
    private String currency;
    private Long planId;
    private Long userId;
}
