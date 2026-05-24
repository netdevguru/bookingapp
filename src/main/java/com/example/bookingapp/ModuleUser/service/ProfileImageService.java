package com.example.bookingapp.ModuleUser.service;

import com.example.bookingapp.ModuleUser.entity.UserEntity;
import com.example.bookingapp.ModuleUser.entity.UserProfileEntity;
import com.example.bookingapp.ModuleUser.repository.UserProfileRepository;
import com.example.bookingapp.ModuleUser.repository.UserRepository;
import com.example.bookingapp.ModuleUser.dto.ProfileImageResponse;
import com.example.bookingapp.ModuleUser.exception.UserException;

import com.example.bookingapp.FileStorage.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class ProfileImageService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Upload or update profile image for a user
     */
    @Transactional
    public ProfileImageResponse uploadProfileImage(Long userId, MultipartFile file) throws UserException {
        // Verify user exists
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with id: " + userId));

        // Validate and store file
        String fileName = fileStorageService.storeProfileImage(userId, file);

        // Get or create user profile
        UserProfileEntity userProfile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfileEntity newProfile = new UserProfileEntity();
                    newProfile.setUser(user);
                    return newProfile;
                });

        // Update avatar path (store the filename in database)
        String avatarPath = "/uploads/profile-images/" + fileName;
        userProfile.setAvatar(avatarPath);

        // Save to database
        userProfileRepository.save(userProfile);

        // Return response with secure API endpoint
        String secureImageUrl = "/api/users/" + userId + "/profile/image/view";
        return new ProfileImageResponse(
                secureImageUrl,
                fileName,
                file.getSize(),
                file.getContentType(),
                LocalDateTime.now()
        );
    }

    /**
     * Delete profile image for a user
     */
    @Transactional
    public void deleteProfileImage(Long userId) throws UserException {
        // Find user profile
        UserProfileEntity userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException("User profile not found"));

        // Delete file from disk
        fileStorageService.deleteProfileImage(userId);

        // Update database
        userProfile.setAvatar(null);
        userProfileRepository.save(userProfile);
    }

    /**
     * Get profile image URL for a user
     */
    public String getProfileImageUrl(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .map(profile -> {
                    if (profile.getAvatar() != null) {
                        // Return API endpoint instead of direct file path
                        return "/api/users/" + userId + "/profile/image/view";
                    }
                    return null;
                })
                .orElse(null);
    }

    /**
     * Load profile image as resource for serving
     */
    public Resource loadProfileImageAsResource(Long userId) throws UserException {
        String fileName = fileStorageService.getProfileImageFileName(userId);
        
        if (fileName == null) {
            throw new UserException("Profile image not found for user: " + userId);
        }

        try {
            Path profileImageLocation = Paths.get("uploads/profile-images").toAbsolutePath().normalize();
            Path filePath = profileImageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new UserException("Profile image not found or not readable");
            }
        } catch (MalformedURLException ex) {
            throw new UserException("Profile image not found");
        }
    }
}
