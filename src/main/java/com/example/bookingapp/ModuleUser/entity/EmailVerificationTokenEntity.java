package com.example.bookingapp.ModuleUser.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private LocalDateTime expiryDate;

    public EmailVerificationTokenEntity(String token, UserEntity user) {
        this.token = token;
        this.user = user;
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}
