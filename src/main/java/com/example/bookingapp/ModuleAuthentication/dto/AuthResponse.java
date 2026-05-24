package com.example.bookingapp.ModuleAuthentication.dto;

import com.example.bookingapp.ModuleUser.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String jwt;
    private boolean status;
    private String message;
    private UserEntity user;
}
