package com.example.bookingapp.ModuleAdmin;

import com.example.bookingapp.ModuleAdmin.service.AdminSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*")
public class PublicSettingsController {

    @Autowired
    private AdminSettingsService adminSettingsService;

    /**
     * Public endpoint to get current currency
     * Accessible to all authenticated users (CUSTOMER, ADMIN, etc.)
     * Read-only - users cannot modify currency
     */
    @GetMapping("/currency")
    public ResponseEntity<Map<String, String>> getPublicCurrency() {
        Map<String, String> response = new HashMap<>();
        response.put("currency", adminSettingsService.getCurrency());
        return ResponseEntity.ok(response);
    }

    /**
     * Get currency symbol for display purposes
     */
    @GetMapping("/currency/symbol")
    public ResponseEntity<Map<String, String>> getCurrencySymbol() {
        String currency = adminSettingsService.getCurrency();
        String symbol = getCurrencySymbolForCode(currency);
        
        Map<String, String> response = new HashMap<>();
        response.put("currency", currency);
        response.put("symbol", symbol);
        return ResponseEntity.ok(response);
    }

    private String getCurrencySymbolForCode(String currency) {
        switch (currency.toUpperCase()) {
            case "USD": return "$";
            case "INR": return "₹";
            case "EUR": return "€";
            case "GBP": return "£";
            case "AUD": return "A$";
            case "CAD": return "C$";
            case "SGD": return "S$";
            case "JPY": return "¥";
            default: return "$";
        }
    }
}
