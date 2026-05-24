package com.example.bookingapp.ModuleSubscription.controller;

import com.example.bookingapp.ModuleSubscription.entity.UserSubscription;
import com.example.bookingapp.ModuleSubscription.enums.SubscriptionStatus;
import com.example.bookingapp.ModuleSubscription.service.UserSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-subscriptions")
@CrossOrigin(origins = "*")
public class UserSubscriptionController {
    
    @Autowired
    private UserSubscriptionService userSubscriptionService;
    
    /**
     * Get all subscriptions (Admin only)
     * GET /api/user-subscriptions
    */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllSubscriptions(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        org.springframework.data.domain.Sort.Direction direction = 
            sortDirection.equalsIgnoreCase("asc") ? 
            org.springframework.data.domain.Sort.Direction.ASC : 
            org.springframework.data.domain.Sort.Direction.DESC;
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, sortBy));
        org.springframework.data.domain.Page<UserSubscription> subscriptionPage = userSubscriptionService.getAllSubscriptions(pageable);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("status", true);
        response.put("subscriptions", subscriptionPage.getContent());
        response.put("currentPage", subscriptionPage.getNumber());
        response.put("totalPages", subscriptionPage.getTotalPages());
        response.put("totalItems", subscriptionPage.getTotalElements());
        response.put("pageSize", subscriptionPage.getSize());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create a new subscription
     * POST /api/user-subscriptions
    */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createSubscription(@RequestBody Map<String, Object> request) {
        // Validate required fields
        if (!request.containsKey("userId") || request.get("userId") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }
        if (!request.containsKey("planId") || request.get("planId") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "planId is required"));
        }
        
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long planId = Long.valueOf(request.get("planId").toString());
            
            String paymentMethod = "credit_card"; // Default
            if (request.containsKey("paymentMethod") && request.get("paymentMethod") != null) {
                paymentMethod = request.get("paymentMethod").toString();
            }
            
            String transactionId = "txn_" + System.currentTimeMillis(); // Default
            if (request.containsKey("transactionId") && request.get("transactionId") != null) {
                transactionId = request.get("transactionId").toString();
            }
            
            UserSubscription subscription = userSubscriptionService.createSubscription(userId, planId, paymentMethod, transactionId);
            return ResponseEntity.ok(subscription);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid userId or planId format"));
        }
    }
    
    /**
     * Get all subscriptions for a user
     * GET /api/user-subscriptions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserSubscription>> getUserSubscriptions(@PathVariable Long userId) {
        List<UserSubscription> subscriptions = userSubscriptionService.getUserSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }
    
    /**
     * Get active subscription for a user
     * GET /api/user-subscriptions/user/{userId}/active
     * Returns the most recent active subscription
     * If multiple active subscriptions exist, returns the newest one
     */
    @GetMapping("/user/{userId}/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getActiveSubscription(@PathVariable Long userId) {
        try {
            System.out.println("Fetching active subscription for user: " + userId);
            
            // Get all active subscriptions to check if there are multiple
            List<UserSubscription> allActive = userSubscriptionService.getAllActiveSubscriptions(userId);
            
            if (allActive.isEmpty()) {
                System.out.println("No active subscription found for user: " + userId);
                Map<String, Object> response = new HashMap<>();
                response.put("hasActiveSubscription", false);
                response.put("message", "No active subscription found");
                response.put("subscription", null);
                return ResponseEntity.ok(response);
            }
            
            // Get the most recent one
            UserSubscription subscription = userSubscriptionService.getActiveSubscription(userId);
            
            System.out.println("Active subscription found for user " + userId + ": " + subscription.getId());
            
            // Warn if multiple active subscriptions exist
            if (allActive.size() > 1) {
                System.out.println("WARNING: User " + userId + " has " + allActive.size() + " active subscriptions. Returning the most recent one.");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasActiveSubscription", true);
            response.put("subscription", subscription);
            response.put("totalActiveSubscriptions", allActive.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error fetching active subscription for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to fetch active subscription: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get all active subscriptions for a user
     * GET /api/user-subscriptions/user/{userId}/active/all
     * Returns all active subscriptions (useful if user has multiple)
     */
    @GetMapping("/user/{userId}/active/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllActiveSubscriptions(@PathVariable Long userId) {
        try {
            System.out.println("Fetching all active subscriptions for user: " + userId);
            List<UserSubscription> subscriptions = userSubscriptionService.getAllActiveSubscriptions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasActiveSubscriptions", !subscriptions.isEmpty());
            response.put("subscriptions", subscriptions);
            response.put("count", subscriptions.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error fetching active subscriptions for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to fetch active subscriptions: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get subscription by id
     * GET /api/user-subscriptions/{subscriptionId}
     */
    @GetMapping("/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserSubscription> getSubscriptionById(@PathVariable Long subscriptionId) {
        UserSubscription subscription = userSubscriptionService.getSubscriptionById(subscriptionId);
        return ResponseEntity.ok(subscription);
    }
    
    /**
     * Cancel a subscription
     * POST /api/user-subscriptions/{subscriptionId}/cancel
     */
    @PostMapping("/{subscriptionId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserSubscription> cancelSubscription(@PathVariable Long subscriptionId) {
        UserSubscription subscription = userSubscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(subscription);
    }
    
    /**
     * Renew a subscription
     * POST /api/user-subscriptions/{subscriptionId}/renew
     */
    @PostMapping("/{subscriptionId}/renew")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserSubscription> renewSubscription(@PathVariable Long subscriptionId) {
        UserSubscription subscription = userSubscriptionService.renewSubscription(subscriptionId);
        return ResponseEntity.ok(subscription);
    }
    
    /**
     * Update subscription status
     * PUT /api/user-subscriptions/{subscriptionId}/status
     */
    @PutMapping("/{subscriptionId}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserSubscription> updateSubscriptionStatus(
            @PathVariable Long subscriptionId,
            @RequestBody Map<String, String> request) {
        SubscriptionStatus status = SubscriptionStatus.valueOf(request.get("status"));
        UserSubscription subscription = userSubscriptionService.updateSubscriptionStatus(subscriptionId, status);
        return ResponseEntity.ok(subscription);
    }
}
