package com.example.bookingapp.ModuleUser;

import com.example.bookingapp.config.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.bookingapp.utils.Mailer;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImplementation implements UserService{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Mailer mailer;

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

        // Send an email containing the reset link
        mailer.sendEmail(
                user.getEmail(),
                "Click the following link to reset your password: http://localhost:5454/reset-password?token=" + resetToken
        );
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
