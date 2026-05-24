package com.example.bookingapp.ModuleAdmin.service;

import com.example.bookingapp.ModuleAdmin.entity.AdminSettings;
import com.example.bookingapp.ModuleAdmin.repository.AdminSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminSettingsService {

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    // Default currency if not set
    private static final String DEFAULT_CURRENCY = "USD";

    public String getCurrency() {
        return getSetting("CURRENCY", DEFAULT_CURRENCY);
    }

    public String getSetting(String key, String defaultValue) {
        return adminSettingsRepository.findBySettingKey(key)
                .map(AdminSettings::getSettingValue)
                .orElse(defaultValue);
    }

    public AdminSettings updateSetting(String key, String value, String description, String updatedBy) {
        AdminSettings setting = adminSettingsRepository.findBySettingKey(key)
                .orElse(new AdminSettings());
        
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setDescription(description);
        setting.setUpdatedBy(updatedBy);
        setting.setUpdatedAt(LocalDateTime.now());
        
        return adminSettingsRepository.save(setting);
    }

    public List<AdminSettings> getAllSettings() {
        return adminSettingsRepository.findAll();
    }

    public AdminSettings getSettingByKey(String key) {
        return adminSettingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));
    }

    public void deleteSetting(String key) {
        AdminSettings setting = getSettingByKey(key);
        adminSettingsRepository.delete(setting);
    }

    // Initialize default settings if they don't exist
    public void initializeDefaultSettings() {
        if (!adminSettingsRepository.existsBySettingKey("CURRENCY")) {
            AdminSettings currency = new AdminSettings();
            currency.setSettingKey("CURRENCY");
            currency.setSettingValue(DEFAULT_CURRENCY);
            currency.setDescription("Default currency for payments (USD, INR, EUR, etc.)");
            currency.setUpdatedAt(LocalDateTime.now());
            currency.setUpdatedBy("SYSTEM");
            adminSettingsRepository.save(currency);
        }
    }
}
