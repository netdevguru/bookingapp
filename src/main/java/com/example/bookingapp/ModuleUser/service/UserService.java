package com.example.bookingapp.ModuleUser.service;

import com.example.bookingapp.ModuleUser.entity.UserEntity;
import com.example.bookingapp.ModuleUser.exception.UserException;

public interface UserService {
    public UserEntity findUserProfileByJwt(String jwt) throws UserException;
    public UserEntity findUserByEmail(String email) throws UserException;
    public UserEntity findUserById(Long userId) throws UserException;
    public UserEntity updateUser(UserEntity user) throws UserException;
    void updatePassword(UserEntity user, String newPassword);
    void sendPasswordResetEmail(UserEntity user);
}
