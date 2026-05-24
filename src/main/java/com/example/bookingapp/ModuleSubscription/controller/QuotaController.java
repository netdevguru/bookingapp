package com.example.bookingapp.ModuleSubscription.controller;

import com.example.bookingapp.ModuleSubscription.entity.UserSubscription;
import com.example.bookingapp.ModuleSubscription.service.QuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/quota")
@CrossOrigin(origins = "*")
public class QuotaController {
    
    @Autowired
    private QuotaService quotaService;
    
    /**
     * Consume tokens from subscription
     * POST /api/quota/consume
     */
    @PostMapping("/consume")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> consumeTokens(@RequestBody Map<String, Object> request) {
        Long subscriptionId = Long.valueOf(request.get("subscriptionId").toString());
        BigDecimal tokens = new BigDecimal(request.get("tokens").toString());
        
        UserSubscription subscription = quotaService.consumeTokens(subscriptionId, tokens);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("subscriptionId", subscription.getId());
        response.put("tokensConsumed", tokens);
        response.put("totalConsumed", subscription.getTokensConsumed());
        response.put("tokensRemaining", subscription.getTokensRemaining());
        response.put("quotaExhausted", subscription.getQuotaExhausted());
        response.put("status", subscription.getStatus());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if subscription has sufficient tokens
     * GET /api/quota/check/{subscriptionId}?tokens=100
     */
    @GetMapping("/check/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> checkTokens(
            @PathVariable Long subscriptionId,
            @RequestParam BigDecimal tokens) {
        
        boolean hasTokens = quotaService.hasTokens(subscriptionId, tokens);
        BigDecimal remaining = quotaService.getRemainingTokens(subscriptionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasTokens", hasTokens);
        response.put("tokensRequired", tokens);
        response.put("tokensRemaining", remaining);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get remaining tokens
     * GET /api/quota/remaining/{subscriptionId}
     */
    @GetMapping("/remaining/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getRemainingTokens(@PathVariable Long subscriptionId) {
        BigDecimal remaining = quotaService.getRemainingTokens(subscriptionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("tokensRemaining", remaining);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get full quota details for a subscription
     * GET /api/quota/details/{subscriptionId}
     */
    @GetMapping("/details/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getQuotaDetails(@PathVariable Long subscriptionId) {
        UserSubscription subscription = quotaService.getSubscriptionQuota(subscriptionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("subscriptionId", subscription.getId());
        response.put("userId", subscription.getUserId());
        response.put("tokenQuota", subscription.getTokenQuota());
        response.put("tokensConsumed", subscription.getTokensConsumed());
        response.put("tokensRemaining", subscription.getTokensRemaining());
        response.put("quotaExhausted", subscription.getQuotaExhausted());
        response.put("status", subscription.getStatus());
        response.put("planName", subscription.getPlan().getPlanName());
        
        // Calculate usage percentage
        if (subscription.getTokenQuota().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal usagePercent = subscription.getTokensConsumed()
                .divide(subscription.getTokenQuota(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            response.put("usagePercentage", usagePercent);
        } else {
            response.put("usagePercentage", 0);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Add tokens to subscription (top-up)
     * POST /api/quota/add-tokens
     */
    @PostMapping("/add-tokens")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> addTokens(@RequestBody Map<String, Object> request) {
        Long subscriptionId = Long.valueOf(request.get("subscriptionId").toString());
        BigDecimal tokens = new BigDecimal(request.get("tokens").toString());
        
        UserSubscription subscription = quotaService.addTokens(subscriptionId, tokens);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("subscriptionId", subscription.getId());
        response.put("tokensAdded", tokens);
        response.put("newQuota", subscription.getTokenQuota());
        response.put("tokensRemaining", subscription.getTokensRemaining());
        response.put("status", subscription.getStatus());
        
        return ResponseEntity.ok(response);
    }
}
