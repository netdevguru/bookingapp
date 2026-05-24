package com.example.bookingapp.config;

import com.example.bookingapp.ModuleUser.UserEntity;
import com.example.bookingapp.ModuleUser.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {
    private final UserRepository userRepository;

    public UserSecurity(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isOwner(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String email = authentication.getName();
        UserEntity currentUser = userRepository.findByEmail(email);
        
        return currentUser != null && currentUser.getId().equals(userId);
    }
}
