package com.example.bookingapp.ModuleSubscription.repository;

import com.example.bookingapp.ModuleSubscription.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
}
