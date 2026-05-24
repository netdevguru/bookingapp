package com.example.bookingapp.ModuleAuthentication.controller;

import com.example.bookingapp.ModuleAuthentication.dto.AuthResponse;
import com.example.bookingapp.ModuleAuthentication.dto.ForgotPasswordRequest;
import com.example.bookingapp.ModuleAuthentication.dto.LoginRequest;
import com.example.bookingapp.ModuleAuthentication.dto.ResetPasswordRequest;
import com.example.bookingapp.ModuleUser.entity.PasswordResetTokenEntity;
import com.example.bookingapp.ModuleUser.repository.PasswordResetTokenRepository;
import com.example.bookingapp.ModuleUser.service.UserService;

import com.example.bookingapp.ModuleUser.exception.UserException;
import com.example.bookingapp.config.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookingapp.ModuleUser.repository.UserRepository;
import com.example.bookingapp.ModuleUser.entity.UserEntity;
import com.example.bookingapp.ModuleUser.service.CustomUserServiceImplementation;
import com.example.bookingapp.ModuleUser.entity.EmailVerificationTokenEntity;
import com.example.bookingapp.ModuleUser.repository.EmailVerificationTokenRepository;
import com.example.bookingapp.ModuleAppointment.events.NotificationEvent;
import com.example.bookingapp.ModuleAppointment.service.EventPublisherService;
import java.time.LocalDateTime;

import java.util.UUID;
import java.util.Optional;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private CustomUserServiceImplementation customUserDetails;

    @Autowired
    private EmailVerificationTokenRepository verificationTokenRepository;

    @Autowired
    private EventPublisherService eventPublisher;



    @Autowired
    private com.example.bookingapp.ModuleUser.service.UserService userService;

    @Autowired
    private com.example.bookingapp.ModuleUser.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping({"/register", "/signup"})
    public ResponseEntity<AuthResponse> register(@RequestBody UserEntity user) throws UserException {
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new UserException("Email is already registered");
        }

        UserEntity createdUser = new UserEntity();
        createdUser.setEmail(user.getEmail());
        createdUser.setFirstName(user.getFirstName());
        createdUser.setLastName(user.getLastName());
        createdUser.setPassword(passwordEncoder.encode(user.getPassword()));
        createdUser.setRole("CUSTOMER");
        createdUser.setActive(false);
        createdUser.setEmailVerified(false);
        userRepository.save(createdUser);

        // Generate verification token
        String verificationToken = UUID.randomUUID().toString();
        EmailVerificationTokenEntity tokenEntity = new EmailVerificationTokenEntity(verificationToken, createdUser);
        verificationTokenRepository.save(tokenEntity);

        // Send verification email
        String verificationUrl = frontendUrl + "/auth/verify-email?token=" + verificationToken;
        String emailBody;
        try {
            emailBody = objectMapper.writeValueAsString(Map.of(
                "firstName", createdUser.getFirstName() != null ? createdUser.getFirstName() : "User",
                "verificationUrl", verificationUrl
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize verification email params", e);
        }
        
        NotificationEvent verificationEvent = NotificationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .notificationType("EMAIL")
            .recipientEmail(createdUser.getEmail())
            .recipientName(createdUser.getFirstName())
            .subject("Verify Your Email - CloudFlows")
            .message(emailBody)
            .appointmentId(null)
            .eventTimestamp(LocalDateTime.now())
            .build();
        eventPublisher.publishNotificationEvent(verificationEvent);

        AuthResponse response = new AuthResponse();
        response.setJwt(null);
        response.setStatus(true);
        response.setMessage("Registration successful! Please check your email to verify your account.");
        response.setUser(createdUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/login", "/signin"})
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) throws UserException {
        String username = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        UserEntity user = userRepository.findByEmail(username);
        
        if (user == null) {
            throw new BadCredentialsException("Invalid username or password");
        }
        
        if (!user.isEmailVerified()) {
            throw new UserException("Please verify your email before logging in. Check your inbox for the verification link.");
        }
        
        if (!user.isActive()) {
            throw new UserException("Your account is not active. Please contact support.");
        }

        Authentication authentication = authenticate(username, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = JwtProvider.generateToken(authentication, user.getId());
        AuthResponse authResponse = new AuthResponse();

        authResponse.setJwt(token);
        authResponse.setStatus(true);
        authResponse.setMessage("Login Success");
        authResponse.setUser(user);

        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }

    @GetMapping("/verify-email")
    @Transactional
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        try {
            System.out.println("Email verification request received for token: " + token);
            
            Optional<EmailVerificationTokenEntity> tokenEntity = verificationTokenRepository.findByToken(token);
            
            if (tokenEntity.isEmpty()) {
                System.out.println("Token not found in database");
                return ResponseEntity.badRequest().body("Invalid verification token");
            }
            
            EmailVerificationTokenEntity verificationToken = tokenEntity.get();
            
            if (verificationToken.isExpired()) {
                System.out.println("Token has expired");
                verificationTokenRepository.delete(verificationToken);
                return ResponseEntity.badRequest().body("Verification token has expired");
            }
            
            UserEntity user = verificationToken.getUser();
            System.out.println("Verifying email for user: " + user.getEmail());
            
            // Check if already verified
            if (user.isEmailVerified()) {
                System.out.println("Email already verified for user: " + user.getEmail());
                verificationTokenRepository.delete(verificationToken);
                return ResponseEntity.ok("Email already verified! You can login to your account.");
            }
            
            // Update user
            user.setEmailVerified(true);
            user.setActive(true);
            userRepository.save(user);
            
            // Delete the used token by ID to avoid detached entity issues
            verificationTokenRepository.deleteById(verificationToken.getId());
            
            // Send welcome email
            try {
                String loginUrl = frontendUrl + "/login";
                String emailBody;
                try {
                    emailBody = objectMapper.writeValueAsString(Map.of(
                        "firstName", user.getFirstName() != null ? user.getFirstName() : "User",
                        "loginUrl", loginUrl
                    ));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize welcome email params", e);
                }
                NotificationEvent welcomeEvent = NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .notificationType("EMAIL")
                    .recipientEmail(user.getEmail())
                    .recipientName(user.getFirstName())
                    .subject("Welcome to CloudFlows!")
                    .message(emailBody)
                    .appointmentId(null)
                    .eventTimestamp(LocalDateTime.now())
                    .build();
                eventPublisher.publishNotificationEvent(welcomeEvent);
                System.out.println("Welcome email sent to: " + user.getEmail());
            } catch (Exception e) {
                System.err.println("Failed to send welcome email to " + user.getEmail() + ": " + e.getMessage());
                // Don't fail verification if email fails
            }
            
            System.out.println("Email verified successfully for user: " + user.getEmail());
            return ResponseEntity.ok("Email verified successfully! You can now login to your account.");
        } catch (Exception e) {
            System.err.println("Error during email verification: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("An error occurred during email verification: " + e.getMessage());
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerificationEmail(@RequestParam("email") String email) throws UserException {
        UserEntity user = userRepository.findByEmail(email);
        
        if (user == null) {
            throw new UserException("User not found");
        }
        
        if (user.isEmailVerified()) {
            return ResponseEntity.badRequest().body("Email is already verified");
        }
        
        // Delete old tokens
        verificationTokenRepository.deleteByUser(user);
        
        // Generate new verification token
        String verificationToken = UUID.randomUUID().toString();
        EmailVerificationTokenEntity tokenEntity = new EmailVerificationTokenEntity(verificationToken, user);
        verificationTokenRepository.save(tokenEntity);
        
        // Send verification email
        String verificationUrl = frontendUrl + "/auth/verify-email?token=" + verificationToken;
        String emailBody;
        try {
            emailBody = objectMapper.writeValueAsString(Map.of(
                "firstName", user.getFirstName() != null ? user.getFirstName() : "User",
                "verificationUrl", verificationUrl
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize verification email params", e);
        }
        
        NotificationEvent verificationEvent = NotificationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .notificationType("EMAIL")
            .recipientEmail(user.getEmail())
            .recipientName(user.getFirstName())
            .subject("Verify Your Email - CloudFlows")
            .message(emailBody)
            .appointmentId(null)
            .eventTimestamp(LocalDateTime.now())
            .build();
        eventPublisher.publishNotificationEvent(verificationEvent);
        
        return ResponseEntity.ok("Verification email sent successfully!");
    }

    private Authentication authenticate(String username, String password) {
        UserDetails userDetails = customUserDetails.loadUserByUsername(username);
        if (userDetails == null) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            UserEntity user = userRepository.findByEmail(request.getEmail());
            
            if (user == null) {
                // Don't reveal if user exists or not for security
                return ResponseEntity.ok("If an account exists with this email, you will receive password reset instructions.");
            }
            
            if (!user.isEmailVerified()) {
                return ResponseEntity.badRequest().body("Please verify your email before resetting password.");
            }
            
            // Delete any existing reset tokens for this user
            com.example.bookingapp.ModuleUser.entity.PasswordResetTokenEntity existingToken = 
                passwordResetTokenRepository.findByUser(user);
            if (existingToken != null) {
                passwordResetTokenRepository.delete(existingToken);
            }
            
            // Generate reset token
            String resetToken = UUID.randomUUID().toString();
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(new java.util.Date());
            cal.add(java.util.Calendar.MINUTE, 30); // 30 minutes expiry
            java.util.Date expiryDate = cal.getTime();
            
            // Save token
            com.example.bookingapp.ModuleUser.entity.PasswordResetTokenEntity passwordResetToken = 
                new com.example.bookingapp.ModuleUser.entity.PasswordResetTokenEntity(user, resetToken, expiryDate);
            passwordResetTokenRepository.save(passwordResetToken);
            
            // Send email
            String resetUrl = frontendUrl + "/auth/reset-password?token=" + resetToken;
            String emailBody;
            try {
                emailBody = objectMapper.writeValueAsString(Map.of(
                    "firstName", user.getFirstName() != null ? user.getFirstName() : "User",
                    "resetUrl", resetUrl
                ));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize reset email params", e);
            }
            NotificationEvent resetEvent = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("EMAIL")
                .recipientEmail(user.getEmail())
                .recipientName(user.getFirstName())
                .subject("Reset Your Password - CloudFlows")
                .message(emailBody)
                .appointmentId(null)
                .eventTimestamp(LocalDateTime.now())
                .build();
            eventPublisher.publishNotificationEvent(resetEvent);
            
            return ResponseEntity.ok("If an account exists with this email, you will receive password reset instructions.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while processing your request.");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            com.example.bookingapp.ModuleUser.entity.PasswordResetTokenEntity tokenEntity = 
                passwordResetTokenRepository.findByToken(request.getToken());
            
            if (tokenEntity == null) {
                return ResponseEntity.badRequest().body("Invalid or expired reset token.");
            }
            
            if (tokenEntity.isExpired()) {
                passwordResetTokenRepository.delete(tokenEntity);
                return ResponseEntity.badRequest().body("Reset token has expired. Please request a new one.");
            }
            
            // Validate password
            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest().body("Password must be at least 6 characters long.");
            }
            
            // Update password
            UserEntity user = tokenEntity.getUser();
            userService.updatePassword(user, request.getNewPassword());
            
            // Delete used token
            passwordResetTokenRepository.delete(tokenEntity);
            
            return ResponseEntity.ok("Password reset successfully! You can now login with your new password.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while resetting your password.");
        }
    }

}
