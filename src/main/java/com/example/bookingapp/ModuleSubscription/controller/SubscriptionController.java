package com.example.bookingapp.ModuleSubscription.controller;

import com.example.bookingapp.ModuleSubscription.service.*;

import com.example.bookingapp.ModuleSubscription.entity.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @Autowired
    private SubscriptionService SubscriptionService;

    @PostMapping
    public SubscriptionEntity createSubscription(@Valid @RequestBody SubscriptionEntity Subscription) {
        return SubscriptionService.saveSubscription(Subscription);
    }

    @GetMapping
    public ResponseEntity<java.util.Map<String, Object>> getAllSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        org.springframework.data.domain.Sort.Direction direction = 
            sortDirection.equalsIgnoreCase("asc") ? 
            org.springframework.data.domain.Sort.Direction.ASC : 
            org.springframework.data.domain.Sort.Direction.DESC;
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, sortBy));
        
        org.springframework.data.domain.Page<SubscriptionEntity> subscriptionPage = 
            SubscriptionService.fetchSubscriptionList(pageable);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("status", true);
        response.put("subscriptions", subscriptionPage.getContent());
        response.put("currentPage", subscriptionPage.getNumber());
        response.put("totalPages", subscriptionPage.getTotalPages());
        response.put("totalItems", subscriptionPage.getTotalElements());
        response.put("pageSize", subscriptionPage.getSize());
        
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public SubscriptionEntity updateSubscription(@RequestBody SubscriptionEntity Subscription, @PathVariable("id") Long id) {
        return SubscriptionService.updateSubscription(Subscription, id);
    }

    @DeleteMapping("/{id}")
    public String deleteSubscription(@PathVariable("id") Long id) {
        SubscriptionService.deleteSubscriptionById(id);
        return "Deleted Successfully";
    }
}
