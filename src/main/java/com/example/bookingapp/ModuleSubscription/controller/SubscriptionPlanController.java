package com.example.bookingapp.ModuleSubscription.controller;

import com.example.bookingapp.ModuleSubscription.entity.SubscriptionPlan;
import com.example.bookingapp.ModuleSubscription.repository.SubscriptionPlanRepository;
import com.example.bookingapp.ModuleSubscription.service.SubscriptionPlanService;
import com.example.bookingapp.ModuleSubscription.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin(origins = "*")
public class SubscriptionPlanController {
    
    @Autowired
    private SubscriptionPlanRepository planRepository;
    
    @Autowired
    private SubscriptionPlanService planService;
    
    /**
     * Get all subscription plans
     * GET /api/plans
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, Object>> getAllPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        org.springframework.data.domain.Sort.Direction direction = 
            sortDirection.equalsIgnoreCase("asc") ? 
            org.springframework.data.domain.Sort.Direction.ASC : 
            org.springframework.data.domain.Sort.Direction.DESC;
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, sortBy));
        
        org.springframework.data.domain.Page<SubscriptionPlan> planPage = planRepository.findAll(pageable);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("status", true);
        response.put("plans", planPage.getContent());
        response.put("currentPage", planPage.getNumber());
        response.put("totalPages", planPage.getTotalPages());
        response.put("totalItems", planPage.getTotalElements());
        response.put("pageSize", planPage.getSize());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get active subscription plans only
     * GET /api/plans/active
     */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SubscriptionPlan>> getActivePlans() {
        List<SubscriptionPlan> plans = planRepository.findByActiveTrue();
        return ResponseEntity.ok(plans);
    }
    
    /**
     * Get subscription plan by ID
     * GET /api/plans/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionPlan> getPlanById(@PathVariable Long id) {
        SubscriptionPlan plan = planRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
        return ResponseEntity.ok(plan);
    }
    
    /**
     * Get subscription plan by name
     * GET /api/plans/name/{planName}
     */
    @GetMapping("/name/{planName}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionPlan> getPlanByName(@PathVariable String planName) {
        SubscriptionPlan plan = planRepository.findByPlanName(planName)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with name: " + planName));
        return ResponseEntity.ok(plan);
    }
    
    /**
     * Create a new subscription plan
     * POST /api/plans
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<SubscriptionPlan> createPlan(@RequestBody SubscriptionPlan plan) {
        SubscriptionPlan savedPlan = planRepository.save(plan);
        return ResponseEntity.ok(savedPlan);
    }
    
    /**
     * Update an existing subscription plan
     * PUT /api/plans/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<SubscriptionPlan> updatePlan(
            @PathVariable Long id,
            @RequestBody SubscriptionPlan planDetails) {
        
        SubscriptionPlan plan = planRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
        
        // Update fields
        plan.setPlanName(planDetails.getPlanName());
        plan.setDescription(planDetails.getDescription());
        plan.setPrice(planDetails.getPrice());
        plan.setBillingCycle(planDetails.getBillingCycle());
        plan.setTrialDays(planDetails.getTrialDays());
        plan.setActive(planDetails.getActive());
        plan.setFeatures(planDetails.getFeatures());
        plan.setTokenAllocation(planDetails.getTokenAllocation());
        plan.setIsQuotaBased(planDetails.getIsQuotaBased());
        
        SubscriptionPlan updatedPlan = planRepository.save(plan);
        return ResponseEntity.ok(updatedPlan);
    }
    
    /**
     * Get plan deletion information (check if safe to delete)
     * GET /api/plans/{id}/deletion-info
     */
    @GetMapping("/{id}/deletion-info")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> getPlanDeletionInfo(@PathVariable Long id) {
        java.util.Map<String, Object> info = planService.getPlanDeletionInfo(id);
        return ResponseEntity.ok(info);
    }
    
    /**
     * Migrate all subscriptions from one plan to another
     * POST /api/plans/{oldPlanId}/migrate/{newPlanId}
     */
    @PostMapping("/{oldPlanId}/migrate/{newPlanId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> migratePlanSubscriptions(
            @PathVariable Long oldPlanId,
            @PathVariable Long newPlanId) {
        
        planService.migratePlanSubscriptions(oldPlanId, newPlanId);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("status", true);
        response.put("message", "Subscriptions migrated successfully");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete a subscription plan (hard delete - only if no subscriptions exist)
     * DELETE /api/plans/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("status", true);
        response.put("message", "Plan deleted successfully");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Soft delete a subscription plan (deactivate it)
     * PATCH /api/plans/{id}/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> deactivatePlan(@PathVariable Long id) {
        SubscriptionPlan plan = planService.softDeletePlan(id);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("status", true);
        response.put("message", "Plan deactivated successfully");
        response.put("plan", plan);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Toggle plan active status
     * PATCH /api/plans/{id}/toggle-active
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<SubscriptionPlan> togglePlanActive(@PathVariable Long id) {
        SubscriptionPlan plan = planRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
        
        plan.setActive(!plan.getActive());
        SubscriptionPlan updatedPlan = planRepository.save(plan);
        return ResponseEntity.ok(updatedPlan);
    }
}
