package com.example.bookingapp.ModuleSubscription.entity;

import com.example.bookingapp.ModuleSubscription.enums.*;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "usage_pricing")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsagePricing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long planId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsageType usageType;
    
    @Column(nullable = false)
    private BigDecimal unitPrice;
    
    private BigDecimal includedQuantity = BigDecimal.ZERO;
    
    private String unit; // e.g., "per 1000 calls", "per GB", "per hour"
    
    @Column(nullable = false)
    private Boolean active = true;
}
