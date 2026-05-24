package com.example.bookingapp.ModuleUser.controller;

import com.example.bookingapp.ModuleUser.entity.UserEntity;
import com.example.bookingapp.ModuleUser.service.ProfileImageService;
import com.example.bookingapp.ModuleUser.service.UserService;
import com.example.bookingapp.ModuleUser.dto.ChangePasswordRequest;
import com.example.bookingapp.ModuleUser.dto.ChangePasswordResponse;
import com.example.bookingapp.ModuleUser.dto.ProfileImageResponse;
import com.example.bookingapp.ModuleUser.exception.InvalidImageException;
import com.example.bookingapp.ModuleUser.exception.UserException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProfileImageService profileImageService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/api/users/profile")
    public ResponseEntity<UserEntity> getUserProfileHandler() throws UserException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        UserEntity user = userService.findUserByEmail(email);
        user.setPassword(null);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PutMapping("/api/users/profile")
    public ResponseEntity<UserEntity> updateUserProfileHandler(@RequestBody UserEntity updatedUser) throws UserException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity user = userService.findUserByEmail(email);
        
        // Update allowed fields (only firstName and lastName can be updated)
        if (updatedUser.getFirstName() != null && !updatedUser.getFirstName().trim().isEmpty()) {
            user.setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getLastName() != null && !updatedUser.getLastName().trim().isEmpty()) {
            user.setLastName(updatedUser.getLastName());
        }
        
        UserEntity savedUser = userService.updateUser(user);
        savedUser.setPassword(null);
        
        return new ResponseEntity<>(savedUser, HttpStatus.OK);
    }

    @PutMapping("/api/users/password")
    public ResponseEntity<ChangePasswordResponse> changePasswordHandler(@RequestBody ChangePasswordRequest request) throws UserException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();            
        UserEntity user = userService.findUserByEmail(email);
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return new ResponseEntity<>(
                new ChangePasswordResponse(false, "Current password is incorrect"),
                HttpStatus.BAD_REQUEST
            );
        }
        
        // Validate new password
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            return new ResponseEntity<>(
                new ChangePasswordResponse(false, "New password must be at least 6 characters long"),
                HttpStatus.BAD_REQUEST
            );
        }
        
        // Update password
        userService.updatePassword(user, request.getNewPassword());
        return new ResponseEntity<>(
            new ChangePasswordResponse(true, "Password changed successfully"),
            HttpStatus.OK
        );
    }

    @GetMapping("/api/users/{userId}")
    @PreAuthorize("hasAuthority('ADMIN') or @userSecurity.isOwner(#userId)")
    public ResponseEntity<UserEntity> findUserById(@PathVariable Long userId) throws UserException {
        UserEntity user = userService.findUserById(userId);
        user.setPassword(null);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    // Profile Image Endpoints

    @PostMapping("/api/users/profile/image")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            UserEntity user = userService.findUserByEmail(email);

            ProfileImageResponse response = profileImageService.uploadProfileImage(user.getId(), file);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Profile image uploaded successfully");
            result.put("data", response);

            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (InvalidImageException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (UserException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to upload profile image: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/api/users/profile/image")
    public ResponseEntity<?> deleteProfileImage() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            UserEntity user = userService.findUserByEmail(email);

            profileImageService.deleteProfileImage(user.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Profile image deleted successfully");

            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (UserException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to delete profile image: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/users/profile/image")
    public ResponseEntity<?> getProfileImageUrl() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            UserEntity user = userService.findUserByEmail(email);

            String imageUrl = profileImageService.getProfileImageUrl(user.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("imageUrl", imageUrl);

            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (UserException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/api/users/{userId}/profile/image/view")
    public ResponseEntity<?> viewProfileImage(@PathVariable Long userId) {
        try {
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            UserEntity authenticatedUser = userService.findUserByEmail(email);

            // Check if user is accessing their own image
            if (!authenticatedUser.getId().equals(userId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "You can only access your own profile image");
                return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
            }

            // Load and return the image file
            Resource resource = profileImageService.loadProfileImageAsResource(userId);

            // Determine content type
            String contentType = "application/octet-stream";
            String filename = resource.getFilename();
            if (filename != null) {
                if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (filename.endsWith(".png")) {
                    contentType = "image/png";
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (UserException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to load profile image: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
