package com.example.bookingapp.ModuleSubscription.repository;

import com.example.bookingapp.ModuleSubscription.entity.*;

import com.example.bookingapp.ModuleSubscription.enums.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
    List<UserSubscription> findByUserId(Long userId);
    List<UserSubscription> findByStatus(SubscriptionStatus status);
    List<UserSubscription> findByEndDateBeforeAndStatus(LocalDateTime date, SubscriptionStatus status);
    List<UserSubscription> findByPlanIdAndStatus(Long planId, SubscriptionStatus status);
    List<UserSubscription> findByPlanId(Long planId);
    
    // Get the most recent active subscription for a user
    Optional<UserSubscription> findFirstByUserIdAndStatusOrderByStartDateDesc(Long userId, SubscriptionStatus status);
}