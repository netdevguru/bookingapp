package com.example.bookingapp.ModuleSubscription.service;

import com.example.bookingapp.ModuleSubscription.entity.UserSubscription;
import com.example.bookingapp.ModuleSubscription.entity.SubscriptionPlan;
import com.example.bookingapp.ModuleSubscription.enums.SubscriptionStatus;
import com.example.bookingapp.ModuleSubscription.exception.ResourceNotFoundException;
import com.example.bookingapp.ModuleSubscription.exception.SubscriptionException;
import com.example.bookingapp.ModuleSubscription.repository.UserSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class QuotaService {
    
    @Autowired
    private UserSubscriptionRepository subscriptionRepository;
    
    /**
     * Consume tokens from user's subscription
     * Automatically expires subscription if quota is exhausted or expired
     */
    @Transactional
    public UserSubscription consumeTokens(Long subscriptionId, BigDecimal tokensToConsume) {
        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        
        // Check if subscription is active
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionException("Subscription is not active. Current status: " + subscription.getStatus());
        }
        
        // Check if subscription has expired (end_date passed)
        if (subscription.getEndDate() != null && subscription.getEndDate().isBefore(java.time.LocalDateTime.now())) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            throw new SubscriptionException("Subscription has expired on " + subscription.getEndDate());
        }
        
        // Check if already exhausted
        if (Boolean.TRUE.equals(subscription.getQuotaExhausted())) {
            throw new SubscriptionException("Token quota already exhausted");
        }
        
        // Calculate new values
        BigDecimal newConsumed = subscription.getTokensConsumed().add(tokensToConsume);
        BigDecimal newRemaining = subscription.getTokenQuota().subtract(newConsumed);
        
        // Check if sufficient tokens available
        if (newRemaining.compareTo(BigDecimal.ZERO) < 0) {
            throw new SubscriptionException("Insufficient tokens. Available: " + 
                subscription.getTokensRemaining() + ", Required: " + tokensToConsume);
        }
        
        // Update subscription
        subscription.setTokensConsumed(newConsumed);
        subscription.setTokensRemaining(newRemaining);
        
        // Check if quota exhausted
        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            subscription.setQuotaExhausted(true);
            subscription.setStatus(SubscriptionStatus.EXPIRED);
        }
        
        return subscriptionRepository.save(subscription);
    }
    
    /**
     * Check if user has sufficient tokens and subscription is not expired
     */
    public boolean hasTokens(Long subscriptionId, BigDecimal tokensRequired) {
        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        
        // Check if expired
        boolean isExpired = subscription.getEndDate() != null 
            && subscription.getEndDate().isBefore(java.time.LocalDateTime.now());
        
        return subscription.getStatus() == SubscriptionStatus.ACTIVE 
            && !isExpired
            && !Boolean.TRUE.equals(subscription.getQuotaExhausted())
            && subscription.getTokensRemaining().compareTo(tokensRequired) >= 0;
    }
    
    /**
     * Get remaining tokens for a subscription
     */
    public BigDecimal getRemainingTokens(Long subscriptionId) {
        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        return subscription.getTokensRemaining();
    }
    
    /**
     * Get subscription with quota details
     */
    public UserSubscription getSubscriptionQuota(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }
    
    /**
     * Initialize quota when subscription is created
     */
    @Transactional
    public void initializeQuota(UserSubscription subscription, SubscriptionPlan plan) {
        if (Boolean.TRUE.equals(plan.getIsQuotaBased())) {
            subscription.setTokenQuota(plan.getTokenAllocation());
            subscription.setTokensConsumed(BigDecimal.ZERO);
            subscription.setTokensRemaining(plan.getTokenAllocation());
            subscription.setQuotaExhausted(false);
        }
    }
    
    /**
     * Add tokens to existing subscription (for top-ups)
     */
    @Transactional
    public UserSubscription addTokens(Long subscriptionId, BigDecimal tokensToAdd) {
        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        
        BigDecimal newQuota = subscription.getTokenQuota().add(tokensToAdd);
        BigDecimal newRemaining = subscription.getTokensRemaining().add(tokensToAdd);
        
        subscription.setTokenQuota(newQuota);
        subscription.setTokensRemaining(newRemaining);
        
        // Reactivate if was exhausted
        if (Boolean.TRUE.equals(subscription.getQuotaExhausted()) && newRemaining.compareTo(BigDecimal.ZERO) > 0) {
            subscription.setQuotaExhausted(false);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }
        
        return subscriptionRepository.save(subscription);
    }
}
