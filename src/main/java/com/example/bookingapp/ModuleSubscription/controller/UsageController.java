package com.example.bookingapp.ModuleSubscription.controller;

import com.example.bookingapp.ModuleSubscription.service.*;

import com.example.bookingapp.ModuleSubscription.entity.*;

import com.example.bookingapp.ModuleSubscription.enums.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usage")
@CrossOrigin(origins = "*")
public class UsageController {
    
    @Autowired
    private UsageService usageService;
    
    @PostMapping("/record")
    public ResponseEntity<UsageMetric> recordUsage(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        Long subscriptionId = Long.valueOf(request.get("subscriptionId").toString());
        UsageType usageType = UsageType.valueOf(request.get("usageType").toString());
        BigDecimal quantity = new BigDecimal(request.get("quantity").toString());
        String description = request.get("description") != null ? request.get("description").toString() : null;
        
        UsageMetric metric = usageService.recordUsage(userId, subscriptionId, usageType, quantity, description);
        return ResponseEntity.ok(metric);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UsageMetric>> getUserUsage(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<UsageMetric> usage = usageService.getUserUsage(userId, start, end);
        return ResponseEntity.ok(usage);
    }
    
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<UsageMetric>> getSubscriptionUsage(
            @PathVariable Long subscriptionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<UsageMetric> usage = usageService.getSubscriptionUsage(subscriptionId, start, end);
        return ResponseEntity.ok(usage);
    }
    
    @GetMapping("/user/{userId}/unbilled-cost")
    public ResponseEntity<Map<String, Double>> getUnbilledCost(@PathVariable Long userId) {
        Double cost = usageService.getTotalUnbilledCost(userId);
        return ResponseEntity.ok(Map.of("unbilledCost", cost));
    }
    
    @PostMapping("/mark-billed")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> markAsBilled(@RequestBody List<Long> usageMetricIds) {
        usageService.markAsBilled(usageMetricIds);
        return ResponseEntity.ok("Usage metrics marked as billed");
    }
    
    // Pricing management endpoints
    @GetMapping("/pricing/plan/{planId}")
    public ResponseEntity<List<UsagePricing>> getPlanPricing(@PathVariable Long planId) {
        List<UsagePricing> pricing = usageService.getPlanPricing(planId);
        return ResponseEntity.ok(pricing);
    }
    
    @PostMapping("/pricing")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UsagePricing> createPricing(@RequestBody UsagePricing pricing) {
        UsagePricing created = usageService.createOrUpdatePricing(pricing);
        return ResponseEntity.ok(created);
    }
    
    @PutMapping("/pricing/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UsagePricing> updatePricing(@PathVariable Long id, @RequestBody UsagePricing pricing) {
        pricing.setId(id);
        UsagePricing updated = usageService.createOrUpdatePricing(pricing);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/pricing/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deletePricing(@PathVariable Long id) {
        usageService.deletePricing(id);
        return ResponseEntity.ok("Pricing deleted successfully");
    }
}
