package com.example.bookingapp.ModuleUser.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChangePasswordResponse {
    private boolean status;
    private String message;
}
