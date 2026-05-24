package com.example.bookingapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConstant {
    
    private static String secretKey;
    
    @Value("${jwt.secret.key}")
    public void setSecretKey(String key) {
        JwtConstant.secretKey = key;
    }
    
    public static String getSecretKey() {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("JWT secret key is not configured. Please set jwt.secret.key in application.properties");
        }
        return secretKey;
    }
    
    public static final String JWT_HEADER = "Authorization";
}
