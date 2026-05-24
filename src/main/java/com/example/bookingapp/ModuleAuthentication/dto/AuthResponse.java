package com.example.bookingapp.ModuleAuthentication;

import com.example.bookingapp.ModuleUser.UserEntity;
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
