package com.example.bookingapp.ModuleSubscription.service;

import com.example.bookingapp.ModuleSubscription.entity.UserSubscription;
import com.example.bookingapp.ModuleSubscription.entity.SubscriptionPlan;
import com.example.bookingapp.ModuleSubscription.enums.SubscriptionStatus;
import com.example.bookingapp.ModuleSubscription.enums.BillingCycle;
import com.example.bookingapp.ModuleSubscription.exception.ResourceNotFoundException;
import com.example.bookingapp.ModuleSubscription.repository.UserSubscriptionRepository;
import com.example.bookingapp.ModuleSubscription.repository.SubscriptionPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserSubscriptionService {
    
    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;
    
    @Autowired
    private SubscriptionPlanRepository planRepository;
    
    @Autowired
    private QuotaService quotaService;
    
    /**
     * Create a new subscription for a user
     */
    @Transactional
    public UserSubscription createSubscription(Long userId, Long planId, String paymentMethod, String transactionId) {
        SubscriptionPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + planId));
        
        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(userId);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setAutoRenew(true);
        subscription.setAmount(plan.getPrice());
        subscription.setPaymentMethod(paymentMethod);
        subscription.setTransactionId(transactionId);
        
        // Handle quota-based vs time-based plans
        if (Boolean.TRUE.equals(plan.getIsQuotaBased())) {
            // Quota-based: Set end date based on validity period
            if (plan.getValidityPeriodMonths() != null && plan.getValidityPeriodMonths() > 0) {
                subscription.setEndDate(LocalDateTime.now().plusMonths(plan.getValidityPeriodMonths()));
            } else {
                // Default to 6 months if not specified
                subscription.setEndDate(LocalDateTime.now().plusMonths(6));
            }
            quotaService.initializeQuota(subscription, plan);
        } else {
            // Time-based: Set end date based on billing cycle
            subscription.setEndDate(calculateEndDate(plan.getBillingCycle()));
        }
        
        // Handle trial period
        if (plan.getTrialDays() != null && plan.getTrialDays() > 0) {
            subscription.setTrialEndDate(LocalDateTime.now().plusDays(plan.getTrialDays()));
        }
        
        return userSubscriptionRepository.save(subscription);
    }
    
    /**
     * Calculate end date based on billing cycle
     */
    private LocalDateTime calculateEndDate(BillingCycle billingCycle) {
        LocalDateTime now = LocalDateTime.now();
        switch (billingCycle) {
            case MONTHLY:
                return now.plusMonths(1);
            case QUARTERLY:
                return now.plusMonths(3);
            case HALF_YEARLY:
                return now.plusMonths(6);
            case YEARLY:
                return now.plusYears(1);
            default:
                return now.plusMonths(1);
        }
    }
    
    /**
     * Get all subscriptions (Admin only)
     */
    public org.springframework.data.domain.Page<UserSubscription> getAllSubscriptions(org.springframework.data.domain.Pageable pageable) {
        return userSubscriptionRepository.findAll(pageable);
    }
    
    /**
     * Get all subscriptions for a user
     */
    public List<UserSubscription> getUserSubscriptions(Long userId) {
        return userSubscriptionRepository.findByUserId(userId);
    }
    
    /**
     * Get active subscription for a user
     * Returns the most recent active subscription if multiple exist
     */
    public UserSubscription getActiveSubscription(Long userId) {
        return userSubscriptionRepository.findFirstByUserIdAndStatusOrderByStartDateDesc(userId, SubscriptionStatus.ACTIVE)
            .orElse(null);
    }
    
    /**
     * Get all active subscriptions for a user
     * Use this if you need to handle multiple active subscriptions
     */
    public List<UserSubscription> getAllActiveSubscriptions(Long userId) {
        return userSubscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
    }
    
    /**
     * Get subscription by id
     */
    public UserSubscription getSubscriptionById(Long subscriptionId) {
        return userSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + subscriptionId));
    }
    
    /**
     * Cancel a subscription
     */
    @Transactional
    public UserSubscription cancelSubscription(Long subscriptionId) {
        UserSubscription subscription = getSubscriptionById(subscriptionId);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setAutoRenew(false);
        return userSubscriptionRepository.save(subscription);
    }
    
    /**
     * Renew a subscription
     */
    @Transactional
    public UserSubscription renewSubscription(Long subscriptionId) {
        UserSubscription subscription = getSubscriptionById(subscriptionId);
        SubscriptionPlan plan = subscription.getPlan();
        
        if (Boolean.TRUE.equals(plan.getIsQuotaBased())) {
            // For quota-based plans, reset the quota
            quotaService.initializeQuota(subscription, plan);
        } else {
            // For time-based plans, extend the end date
            subscription.setEndDate(calculateEndDate(plan.getBillingCycle()));
        }
        
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());
        
        return userSubscriptionRepository.save(subscription);
    }
    
    /**
     * Update subscription status
     */
    @Transactional
    public UserSubscription updateSubscriptionStatus(Long subscriptionId, SubscriptionStatus status) {
        UserSubscription subscription = getSubscriptionById(subscriptionId);
        subscription.setStatus(status);
        return userSubscriptionRepository.save(subscription);
    }
}
