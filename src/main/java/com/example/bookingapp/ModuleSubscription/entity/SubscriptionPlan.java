package com.example.bookingapp.ModuleSubscription.entity;

import com.example.bookingapp.ModuleSubscription.enums.*;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String planName;
    
    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycle billingCycle;
    
    @Column(nullable = false)
    private Integer trialDays = 0;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    private String features; // JSON or comma-separated features
    
    // Quota-based plan fields
    @Column(name = "token_allocation")
    private BigDecimal tokenAllocation = BigDecimal.ZERO;
    
    @Column(name = "is_quota_based")
    private Boolean isQuotaBased = false;
    
    @Column(name = "validity_period_months")
    private Integer validityPeriodMonths; // For quota-based plans: how long tokens are valid (e.g., 6 months)
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }   
}
