package com.example.bookingapp.ModuleAdmin;

import com.example.bookingapp.ModuleAdmin.dto.AdminSettingsDTO;
import com.example.bookingapp.ModuleAdmin.entity.AdminSettings;
import com.example.bookingapp.ModuleAdmin.service.AdminSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@CrossOrigin(origins = "*")
public class AdminSettingsController {

    @Autowired
    private AdminSettingsService adminSettingsService;

    @GetMapping
    public ResponseEntity<List<AdminSettings>> getAllSettings() {
        return ResponseEntity.ok(adminSettingsService.getAllSettings());
    }

    @GetMapping("/{key}")
    public ResponseEntity<AdminSettings> getSettingByKey(@PathVariable String key) {
        try {
            return ResponseEntity.ok(adminSettingsService.getSettingByKey(key));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/currency")
    public ResponseEntity<Map<String, String>> getCurrency() {
        Map<String, String> response = new HashMap<>();
        response.put("currency", adminSettingsService.getCurrency());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<AdminSettings> updateSetting(
            @RequestBody AdminSettingsDTO dto,
            Authentication authentication
    ) {
        String updatedBy = authentication.getName();
        AdminSettings updated = adminSettingsService.updateSetting(
                dto.getSettingKey(),
                dto.getSettingValue(),
                dto.getDescription(),
                updatedBy
        );
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/currency")
    public ResponseEntity<Map<String, String>> updateCurrency(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        String currency = request.get("currency");
        if (currency == null || currency.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Currency is required"));
        }

        // Validate currency code (basic validation)
        if (!currency.matches("^[A-Z]{3}$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid currency code. Must be 3 uppercase letters (e.g., USD, INR, EUR)"));
        }

        String updatedBy = authentication.getName();
        adminSettingsService.updateSetting(
                "CURRENCY",
                currency.toUpperCase(),
                "Default currency for payments",
                updatedBy
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "Currency updated successfully");
        response.put("currency", currency.toUpperCase());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Map<String, String>> deleteSetting(@PathVariable String key) {
        try {
            adminSettingsService.deleteSetting(key);
            return ResponseEntity.ok(Map.of("message", "Setting deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/initialize")
    public ResponseEntity<Map<String, String>> initializeDefaultSettings() {
        adminSettingsService.initializeDefaultSettings();
        return ResponseEntity.ok(Map.of("message", "Default settings initialized"));
    }
}
