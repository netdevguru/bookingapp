package com.example.bookingapp.ModuleSubscription.service;

import com.example.bookingapp.ModuleSubscription.repository.*;

import com.example.bookingapp.ModuleSubscription.entity.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository SubscriptionRepository;

    public org.springframework.data.domain.Page<SubscriptionEntity> fetchSubscriptionList(org.springframework.data.domain.Pageable pageable) {
        return SubscriptionRepository.findAll(pageable);
    }

    public SubscriptionEntity saveSubscription(SubscriptionEntity Subscription) {
        return SubscriptionRepository.save(Subscription);
    }

    public SubscriptionEntity updateSubscription(SubscriptionEntity Subscription, Long id) {
        SubscriptionEntity dbSubscription = SubscriptionRepository.findById(id).orElse(null);
        if (dbSubscription != null) {
            // Set fields as necessary
            return SubscriptionRepository.save(dbSubscription);
        }
        return null; // or handle this case as needed
    }

    public void deleteSubscriptionById(Long id) {
        SubscriptionRepository.deleteById(id);
    }
}
