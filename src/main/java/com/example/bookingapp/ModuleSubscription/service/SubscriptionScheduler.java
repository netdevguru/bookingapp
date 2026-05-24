package com.example.bookingapp.ModuleSubscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionPlanService subscriptionPlanService;

    // Run every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkExpiredSubscriptions() {
        log.info("Running scheduled task to check expired subscriptions");
        subscriptionPlanService.checkExpiredSubscriptions();
    }
}
