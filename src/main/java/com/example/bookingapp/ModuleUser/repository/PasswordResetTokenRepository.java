package com.example.bookingapp.ModuleUser.repository;

import com.example.bookingapp.ModuleUser.entity.PasswordResetTokenEntity;
import com.example.bookingapp.ModuleUser.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Integer> {
    PasswordResetTokenEntity findByToken(String token);
    PasswordResetTokenEntity findByUser(UserEntity user);
}

