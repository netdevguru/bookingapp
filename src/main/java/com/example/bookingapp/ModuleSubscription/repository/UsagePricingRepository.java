package com.example.bookingapp.ModuleSubscription.repository;

import com.example.bookingapp.ModuleSubscription.entity.*;

import com.example.bookingapp.ModuleSubscription.enums.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsagePricingRepository extends JpaRepository<UsagePricing, Long> {
    List<UsagePricing> findByPlanId(Long planId);
    
    Optional<UsagePricing> findByPlanIdAndUsageType(Long planId, UsageType usageType);
    
    List<UsagePricing> findByPlanIdAndActiveTrue(Long planId);
}
