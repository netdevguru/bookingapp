package com.example.bookingapp.ModuleAdmin.controller;

import com.example.bookingapp.ModuleUser.entity.UserEntity;
import com.example.bookingapp.ModuleUser.repository.UserRepository;

import com.example.bookingapp.ModuleAppointment.events.NotificationEvent;
import com.example.bookingapp.ModuleAppointment.service.EventPublisherService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisherService eventPublisher;
    
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        org.springframework.data.domain.Sort.Direction direction =
            sortDirection.equalsIgnoreCase("asc") ?
            org.springframework.data.domain.Sort.Direction.ASC :
            org.springframework.data.domain.Sort.Direction.DESC;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, sortBy));
        org.springframework.data.domain.Page<UserEntity> userPage = userRepository.findAll(pageable);

        // Remove passwords from response
        userPage.getContent().forEach(user -> user.setPassword(null));

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("users", userPage.getContent());
        response.put("currentPage", userPage.getNumber());
        response.put("totalPages", userPage.getTotalPages());
        response.put("totalItems", userPage.getTotalElements());
        response.put("pageSize", userPage.getSize());
        response.put("message", "Users fetched successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody UserEntity user) {
        // Check if email already exists
        if (userRepository.findByEmail(user.getEmail()) != null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", false);
            errorResponse.put("message", "Email is already registered");
            return ResponseEntity.status(400).body(errorResponse);
        }

        UserEntity newUser = new UserEntity();
        newUser.setEmail(user.getEmail());
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setRole(user.getRole() != null ? user.getRole() : "CUSTOMER");
        newUser.setActive(true); // Admin-created users are active by default
        newUser.setEmailVerified(true); // Admin-created users are verified by default

        UserEntity savedUser = userRepository.save(newUser);
        
        // Send welcome email
        try {
            String loginUrl = frontendUrl + "/login";
            String emailBody = objectMapper.writeValueAsString(Map.of(
                "firstName", savedUser.getFirstName() != null ? savedUser.getFirstName() : "User",
                "loginUrl", loginUrl
            ));
            NotificationEvent welcomeEvent = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("EMAIL")
                .recipientEmail(savedUser.getEmail())
                .recipientName(savedUser.getFirstName())
                .subject("Welcome to CloudFlows!")
                .message(emailBody)
                .appointmentId(null)
                .eventTimestamp(LocalDateTime.now())
                .build();
            eventPublisher.publishNotificationEvent(welcomeEvent);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email to " + savedUser.getEmail() + ": " + e.getMessage());
            // Don't fail user creation if email fails
        }
        
        savedUser.setPassword(null);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("user", savedUser);
        response.put("message", "User created successfully");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
        @PathVariable Long userId,
        @RequestBody UserEntity updatedUser
    ) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Update user fields (don't update password or email through this endpoint)
        if (updatedUser.getFirstName() != null) {
            user.setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getLastName() != null) {
            user.setLastName(updatedUser.getLastName());
        }
        if (updatedUser.getRole() != null) {
            user.setRole(updatedUser.getRole());
        }
        if (updatedUser.isActive() != user.isActive()) {
            user.setActive(updatedUser.isActive());
        }
        if (updatedUser.isEmailVerified() != user.isEmailVerified()) {
            user.setEmailVerified(updatedUser.isEmailVerified());
        }

        userRepository.save(user);
        user.setPassword(null);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("user", user);
        response.put("message", "User updated successfully");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
        @PathVariable Long userId,
        @RequestBody Map<String, String> request
    ) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        String newRole = request.get("role");
        user.setRole(newRole);
        userRepository.save(user);
        user.setPassword(null);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("user", user);
        response.put("message", "User role updated successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-user-role")
    public ResponseEntity<Map<String, Object>> updateUserRoleByEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newRole = request.get("role");

        UserEntity user = userRepository.findByEmail(email);
        if (user == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", false);
            errorResponse.put("message", "User not found");
            return ResponseEntity.status(404).body(errorResponse);
        }

        user.setRole(newRole);
        userRepository.save(user);

        user.setPassword(null);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("user", user);
        response.put("message", "User role updated successfully");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long userId) {
        userRepository.deleteById(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User deleted successfully");

        return ResponseEntity.ok(response);
    }
}
