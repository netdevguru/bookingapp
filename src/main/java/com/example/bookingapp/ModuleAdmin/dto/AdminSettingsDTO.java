package com.example.bookingapp.ModuleAdmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSettingsDTO {
    private String settingKey;
    private String settingValue;
    private String description;
}
