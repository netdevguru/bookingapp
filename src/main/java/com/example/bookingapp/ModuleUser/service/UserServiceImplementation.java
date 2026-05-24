package com.example.bookingapp.ModuleUser;

import com.example.bookingapp.config.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.bookingapp.ModuleAppointment.events.NotificationEvent;
import com.example.bookingapp.ModuleAppointment.service.EventPublisherService;
import java.time.LocalDateTime;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserServiceImplementation implements UserService{
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EventPublisherService eventPublisher;

    @Override
    public UserEntity findUserProfileByJwt(String jwt) throws UserException {
        String email = JwtProvider.getEmailFromJwtToken(jwt);
        UserEntity user = userRepository.findByEmail(email);
        userRepository.save(user);

        if (user == null) {
            throw new UserException("user not exist with email " + email);
        }
        return user;
    }

    @Override
    public UserEntity findUserByEmail(String username) throws UserException {
        UserEntity user = userRepository.findByEmail(username);
        if (user != null) {
            return user;
        }
        throw new UserException("user not exist with username " + username);
    }

    @Override
    public UserEntity findUserById(Long userId) throws UserException {
        Optional<UserEntity> opt = userRepository.findById(userId);

        if (opt.isEmpty()) {
            throw new UserException("user not found with id " + userId);
        }
        return opt.get();
    }

    @Override
    public UserEntity updateUser(UserEntity user) throws UserException {
        if (user.getId() == null) {
            throw new UserException("User ID cannot be null");
        }
        return userRepository.save(user);
    }

    @Override
    public void updatePassword(UserEntity user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void sendPasswordResetEmail(UserEntity user) {

        // Generate a random token (you might want to use a library for this)
        String resetToken = generateRandomToken();

        // Calculate expiry date
        Date expiryDate = calculateExpiryDate();

        // Save the token in the database
        PasswordResetTokenEntity passwordResetToken = new PasswordResetTokenEntity(user, resetToken, expiryDate);
        passwordResetTokenRepository.save(passwordResetToken);

        // Send an email containing the reset link via Kafka
        String resetUrl = "http://localhost:5454/reset-password?token=" + resetToken;
        String emailBody;
        try {
            emailBody = objectMapper.writeValueAsString(Map.of(
                "firstName", user.getFirstName() != null ? user.getFirstName() : "User",
                "resetUrl", resetUrl
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize reset email params", e);
        }
        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("EMAIL")
                .recipientEmail(user.getEmail())
                .recipientName(user.getFirstName())
                .subject("Password Reset Request")
                .message(emailBody)
                .appointmentId(null)
                .eventTimestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishNotificationEvent(event);
    }

    private String generateRandomToken() {
        return UUID.randomUUID().toString();
    }

    private Date calculateExpiryDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, 10);
        return cal.getTime();
    }
}
