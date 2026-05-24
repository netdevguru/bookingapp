package com.example.bookingapp.ModuleUser;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Integer> {
    PasswordResetTokenEntity findByToken(String token);
    PasswordResetTokenEntity findByUser(UserEntity user);
}

