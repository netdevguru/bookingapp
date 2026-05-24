package com.example.bookingapp.ModuleUser.repository;

import com.example.bookingapp.ModuleUser.entity.UserProfileEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {
    Optional<UserProfileEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
