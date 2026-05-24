package com.example.bookingapp.ModuleSubscription.service;

import com.example.bookingapp.ModuleSubscription.exception.*;

import com.example.bookingapp.ModuleSubscription.repository.*;

import com.example.bookingapp.ModuleSubscription.entity.*;

import com.example.bookingapp.ModuleSubscription.enums.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsageService {
    
    @Autowired
    private UsageMetricRepository usageMetricRepository;
    
    @Autowired
    private UsagePricingRepository usagePricingRepository;
    
    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;
    
    @Transactional
    public UsageMetric recordUsage(Long userId, Long subscriptionId, UsageType usageType, BigDecimal quantity, String description) {
        // Get the user's subscription
        UserSubscription subscription = userSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        
        // Get pricing for this usage type
        UsagePricing pricing = usagePricingRepository
            .findByPlanIdAndUsageType(subscription.getPlan().getId(), usageType)
            .orElseThrow(() -> new SubscriptionException("No pricing found for usage type: " + usageType));
        
        // Calculate cost (subtract included quantity if any)
        BigDecimal billableQuantity = quantity.subtract(pricing.getIncludedQuantity()).max(BigDecimal.ZERO);
        BigDecimal totalCost = billableQuantity.multiply(pricing.getUnitPrice());
        
        // Create usage metric
        UsageMetric metric = new UsageMetric();
        metric.setUserId(userId);
        metric.setSubscriptionId(subscriptionId);
        metric.setUsageType(usageType);
        metric.setQuantity(quantity);
        metric.setUnitPrice(pricing.getUnitPrice());
        metric.setTotalCost(totalCost);
        metric.setDescription(description);
        metric.setBillingPeriodStart(subscription.getStartDate());
        metric.setBillingPeriodEnd(subscription.getEndDate());
        metric.setBilled(false);
        
        return usageMetricRepository.save(metric);
    }
    
    public List<UsageMetric> getUserUsage(Long userId, LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            return usageMetricRepository.findByUserIdAndDateRange(userId, start, end);
        }
        return usageMetricRepository.findByUserIdAndBilledFalse(userId);
    }
    
    public List<UsageMetric> getSubscriptionUsage(Long subscriptionId, LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            return usageMetricRepository.findBySubscriptionIdAndDateRange(subscriptionId, start, end);
        }
        return usageMetricRepository.findBySubscriptionIdAndBilledFalse(subscriptionId);
    }
    
    public Double getTotalUnbilledCost(Long userId) {
        Double cost = usageMetricRepository.getTotalUnbilledCostByUserId(userId);
        return cost != null ? cost : 0.0;
    }
    
    @Transactional
    public void markAsBilled(List<Long> usageMetricIds) {
        List<UsageMetric> metrics = usageMetricRepository.findAllById(usageMetricIds);
        metrics.forEach(metric -> metric.setBilled(true));
        usageMetricRepository.saveAll(metrics);
    }
    
    public List<UsagePricing> getPlanPricing(Long planId) {
        return usagePricingRepository.findByPlanIdAndActiveTrue(planId);
    }
    
    @Transactional
    public UsagePricing createOrUpdatePricing(UsagePricing pricing) {
        return usagePricingRepository.save(pricing);
    }
    
    @Transactional
    public void deletePricing(Long pricingId) {
        usagePricingRepository.deleteById(pricingId);
    }
}
