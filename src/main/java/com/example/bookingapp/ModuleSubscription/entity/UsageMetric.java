package com.example.bookingapp.ModuleSubscription.entity;

import com.example.bookingapp.ModuleSubscription.enums.*;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private Long subscriptionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsageType usageType;
    
    @Column(nullable = false)
    private BigDecimal quantity;
    
    @Column(nullable = false)
    private BigDecimal unitPrice;
    
    @Column(nullable = false)
    private BigDecimal totalCost;
    
    private String description;
    
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
    
    @Column(name = "billing_period_start")
    private LocalDateTime billingPeriodStart;
    
    @Column(name = "billing_period_end")
    private LocalDateTime billingPeriodEnd;
    
    @Column(nullable = false)
    private Boolean billed = false;
    
    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
        if (totalCost == null && quantity != null && unitPrice != null) {
            totalCost = quantity.multiply(unitPrice);
        }
    }
}
