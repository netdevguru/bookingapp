package com.example.bookingapp.ModuleSubscription.dto;

import com.example.bookingapp.ModuleSubscription.enums.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanRequest {
    private String planName;
    private String description;
    private BigDecimal price;
    private BillingCycle billingCycle;
    private Integer trialDays;
    private Boolean active;
    private String features;
}

