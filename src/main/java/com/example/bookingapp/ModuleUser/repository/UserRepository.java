package com.example.bookingapp.ModuleUser.repository;

import com.example.bookingapp.ModuleUser.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByEmail(String email);
}