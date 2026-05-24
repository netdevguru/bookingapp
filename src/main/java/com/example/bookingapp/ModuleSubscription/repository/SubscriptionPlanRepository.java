package com.example.bookingapp.ModuleSubscription.repository;

import com.example.bookingapp.ModuleSubscription.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    List<SubscriptionPlan> findByActiveTrue();
    Optional<SubscriptionPlan> findByPlanName(String planName);
}