package com.example.bookingapp.ModuleUser.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
}
