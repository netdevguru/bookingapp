package com.example.bookingapp.ModuleSubscription.repository;

import com.example.bookingapp.ModuleSubscription.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UsageMetricRepository extends JpaRepository<UsageMetric, Long> {
    List<UsageMetric> findByUserIdAndBilledFalse(Long userId);
    
    List<UsageMetric> findBySubscriptionIdAndBilledFalse(Long subscriptionId);
    
    @Query("SELECT u FROM UsageMetric u WHERE u.userId = ?1 AND u.recordedAt BETWEEN ?2 AND ?3")
    List<UsageMetric> findByUserIdAndDateRange(Long userId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT u FROM UsageMetric u WHERE u.subscriptionId = ?1 AND u.recordedAt BETWEEN ?2 AND ?3")
    List<UsageMetric> findBySubscriptionIdAndDateRange(Long subscriptionId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT SUM(u.totalCost) FROM UsageMetric u WHERE u.userId = ?1 AND u.billed = false")
    Double getTotalUnbilledCostByUserId(Long userId);
}
