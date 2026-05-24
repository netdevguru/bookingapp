package com.example.bookingapp.ModuleSubscription.service;

import com.example.bookingapp.ModuleSubscription.dto.*;
import com.example.bookingapp.ModuleSubscription.exception.*;
import com.example.bookingapp.ModuleSubscription.repository.*;
import com.example.bookingapp.ModuleSubscription.entity.*;
import com.example.bookingapp.ModuleSubscription.enums.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    // Create a new plan
    public SubscriptionPlan createPlan(SubscriptionPlanRequest request) {
        SubscriptionPlan plan = new SubscriptionPlan();

        plan.setPlanName(request.getPlanName());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setBillingCycle(request.getBillingCycle());
        plan.setTrialDays(request.getTrialDays() != null ? request.getTrialDays() : 0);
        plan.setActive(request.getActive() != null ? request.getActive() : true);
        plan.setFeatures(request.getFeatures());
        
        return subscriptionPlanRepository.save(plan);
    }

    // Update existing subscription plan
    public SubscriptionPlan updatePlan(Long planId, SubscriptionPlanRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId).orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        
        plan.setPlanName(request.getPlanName());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setBillingCycle(request.getBillingCycle());
        plan.setTrialDays(request.getTrialDays());
        plan.setActive(request.getActive());
        plan.setFeatures(request.getFeatures());
        
        return subscriptionPlanRepository.save(plan);
    }

    public List<SubscriptionPlan> getAllActivePlans() {
        return subscriptionPlanRepository.findByActiveTrue();
    }

    public SubscriptionPlan getPlanById(Long planId) {
        return subscriptionPlanRepository.findById(planId).orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
    }
    
    @Transactional
    public void deletePlan(Long planId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        
        // Check if plan has ANY subscriptions (active, expired, cancelled, trial)
        List<UserSubscription> allSubscriptions = userSubscriptionRepository.findByPlanId(planId);
        
        if (!allSubscriptions.isEmpty()) {
            // Count by status
            long activeCount = allSubscriptions.stream()
                .filter(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE)
                .count();
            long trialCount = allSubscriptions.stream()
                .filter(sub -> sub.getStatus() == SubscriptionStatus.TRIAL)
                .count();
            long otherCount = allSubscriptions.size() - activeCount - trialCount;
            
            StringBuilder message = new StringBuilder();
            message.append(String.format("Cannot delete '%s' plan. ", plan.getPlanName()));
            
            if (activeCount > 0) {
                message.append(String.format("%d active subscription%s", activeCount, activeCount == 1 ? "" : "s"));
            }
            if (trialCount > 0) {
                if (activeCount > 0) message.append(", ");
                message.append(String.format("%d trial subscription%s", trialCount, trialCount == 1 ? "" : "s"));
            }
            if (otherCount > 0) {
                if (activeCount > 0 || trialCount > 0) message.append(", and ");
                message.append(String.format("%d other subscription%s", otherCount, otherCount == 1 ? "" : "s"));
            }
            
            message.append(" are using this plan. Please remove all subscription references before deleting the plan.");
            
            throw new SubscriptionException(message.toString());
        }
        
        subscriptionPlanRepository.delete(plan);
        log.info("Deleted plan: {} ({})", plan.getPlanName(), planId);
    }
    
    @Transactional
    public SubscriptionPlan softDeletePlan(Long planId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        
        plan.setActive(false);
        log.info("Soft deleted (deactivated) plan: {} ({})", plan.getPlanName(), planId);
        return subscriptionPlanRepository.save(plan);
    }
    
    @Transactional
    public void migratePlanSubscriptions(Long oldPlanId, Long newPlanId) {
        SubscriptionPlan oldPlan = subscriptionPlanRepository.findById(oldPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Old plan not found"));
        
        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(newPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("New plan not found"));
        
        if (!newPlan.getActive()) {
            throw new SubscriptionException("Cannot migrate to an inactive plan");
        }
        
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByPlanId(oldPlanId);
        
        for (UserSubscription subscription : subscriptions) {
            subscription.setPlan(newPlan);
            // Optionally adjust pricing
            subscription.setAmount(newPlan.getPrice());
            userSubscriptionRepository.save(subscription);
        }
        
        log.info("Migrated {} subscriptions from plan {} to plan {}", 
                subscriptions.size(), oldPlan.getPlanName(), newPlan.getPlanName());
    }
    
    @Transactional
    public java.util.Map<String, Object> getPlanDeletionInfo(Long planId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        
        List<UserSubscription> allSubscriptions = userSubscriptionRepository.findByPlanId(planId);
        
        long activeCount = allSubscriptions.stream()
            .filter(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE)
            .count();
        long trialCount = allSubscriptions.stream()
            .filter(sub -> sub.getStatus() == SubscriptionStatus.TRIAL)
            .count();
        long expiredCount = allSubscriptions.stream()
            .filter(sub -> sub.getStatus() == SubscriptionStatus.EXPIRED)
            .count();
        long cancelledCount = allSubscriptions.stream()
            .filter(sub -> sub.getStatus() == SubscriptionStatus.CANCELLED)
            .count();
        
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        info.put("planId", planId);
        info.put("planName", plan.getPlanName());
        info.put("isActive", plan.getActive());
        info.put("totalSubscriptions", allSubscriptions.size());
        info.put("activeSubscriptions", activeCount);
        info.put("trialSubscriptions", trialCount);
        info.put("expiredSubscriptions", expiredCount);
        info.put("cancelledSubscriptions", cancelledCount);
        info.put("canHardDelete", allSubscriptions.isEmpty());
        info.put("recommendedAction", allSubscriptions.isEmpty() ? "HARD_DELETE" : "SOFT_DELETE");
        
        return info;
    }

    // Subscription Management
    @Transactional
    public UserSubscription subscribe(SubscribeRequest request) {
        // Check if user already has active subscription
        List<UserSubscription> activeSubscriptions = userSubscriptionRepository
            .findByUserIdAndStatus(request.getUserId(), SubscriptionStatus.ACTIVE);
        
        if (!activeSubscriptions.isEmpty()) {
            throw new SubscriptionException("User already has an active subscription");
        }

        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getPlanId()).orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        if (!plan.getActive()) {
            throw new SubscriptionException("Plan is not active");
        }

        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(request.getUserId());
        subscription.setPlan(plan);
        subscription.setAmount(plan.getPrice());
        subscription.setPaymentMethod(request.getPaymentMethod());
        subscription.setTransactionId(request.getTransactionId());
        subscription.setStartDate(LocalDateTime.now());
        subscription.setAutoRenew(true);

        // Calculate end date based on billing cycle
        LocalDateTime endDate = calculateEndDate(LocalDateTime.now(), plan.getBillingCycle());
        subscription.setEndDate(endDate);

        // Set trial period if applicable
        if (plan.getTrialDays() > 0) {
            subscription.setStatus(SubscriptionStatus.TRIAL);
            subscription.setTrialEndDate(LocalDateTime.now().plusDays(plan.getTrialDays()));
        } else {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        log.info("User {} subscribed to plan {}", request.getUserId(), plan.getPlanName());
        return userSubscriptionRepository.save(subscription);
    }

    public UserSubscription cancelSubscription(Long subscriptionId) {
        UserSubscription subscription = userSubscriptionRepository.findById(subscriptionId).orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setAutoRenew(false);

        log.info("Subscription {} cancelled", subscriptionId);
        return userSubscriptionRepository.save(subscription);
    }

    public UserSubscription renewSubscription(Long subscriptionId) {
        UserSubscription subscription = userSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        if (subscription.getStatus() != SubscriptionStatus.EXPIRED) {
            throw new SubscriptionException("Only expired subscriptions can be renewed");
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(calculateEndDate(LocalDateTime.now(), 
                subscription.getPlan().getBillingCycle()));
        subscription.setAutoRenew(true);

        log.info("Subscription {} renewed", subscriptionId);
        return userSubscriptionRepository.save(subscription);
    }

    public UserSubscription getUserActiveSubscription(Long userId) {
        List<UserSubscription> activeSubscriptions = userSubscriptionRepository
            .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
        
        if (activeSubscriptions.isEmpty()) {
            throw new ResourceNotFoundException("No active subscription found");
        }
        
        // Return the most recent one if multiple exist
        return activeSubscriptions.stream()
            .max((s1, s2) -> s1.getStartDate().compareTo(s2.getStartDate()))
            .orElseThrow(() -> new ResourceNotFoundException("No active subscription found"));
    }

    public List<UserSubscription> getUserSubscriptionHistory(Long userId) {
        return userSubscriptionRepository.findByUserId(userId);
    }

    // Check and update expired subscriptions
    @Transactional
    public void checkExpiredSubscriptions() {
        List<UserSubscription> expiring = userSubscriptionRepository
                .findByEndDateBeforeAndStatus(LocalDateTime.now(), SubscriptionStatus.ACTIVE);

        for (UserSubscription sub : expiring) {
            if (sub.getAutoRenew()) {
                // Auto-renew logic (integrate with payment gateway)
                sub.setStartDate(sub.getEndDate());
                sub.setEndDate(calculateEndDate(sub.getEndDate(), sub.getPlan().getBillingCycle()));
                log.info("Auto-renewed subscription {}", sub.getId());
            } else {
                sub.setStatus(SubscriptionStatus.EXPIRED);
                log.info("Subscription {} expired", sub.getId());
            }
            userSubscriptionRepository.save(sub);
        }
    }

    // Helper method to calculate end date
    private LocalDateTime calculateEndDate(LocalDateTime startDate, BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> startDate.plusMonths(1);
            case QUARTERLY -> startDate.plusMonths(3);
            case HALF_YEARLY -> startDate.plusMonths(6);
            case YEARLY -> startDate.plusYears(1);
        };
    }
}