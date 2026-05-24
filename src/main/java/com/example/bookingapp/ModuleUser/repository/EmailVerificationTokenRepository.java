package com.example.bookingapp.ModuleUser.repository;

import com.example.bookingapp.ModuleUser.entity.EmailVerificationTokenEntity;
import com.example.bookingapp.ModuleUser.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenEntity, Long> {
    Optional<EmailVerificationTokenEntity> findByToken(String token);
    void deleteByUser(UserEntity user);
}
