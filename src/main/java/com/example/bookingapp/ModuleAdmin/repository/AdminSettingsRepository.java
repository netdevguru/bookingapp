package com.example.bookingapp.ModuleAdmin.repository;

import com.example.bookingapp.ModuleAdmin.entity.AdminSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminSettingsRepository extends JpaRepository<AdminSettings, Long> {
    Optional<AdminSettings> findBySettingKey(String settingKey);
    boolean existsBySettingKey(String settingKey);
}
