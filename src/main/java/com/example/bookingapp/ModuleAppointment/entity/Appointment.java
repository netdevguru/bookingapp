package com.example.bookingapp.ModuleAppointment.entity;

import com.example.bookingapp.ModuleAppointment.enums.AppointmentStatus;
import com.example.bookingapp.ModuleUser.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private AppointmentSlot slot;
    
    @Column(nullable = false)
    private LocalDateTime appointmentDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;
    
    @Column(length = 2000)
    private String symptoms;
    
    @Column(length = 2000)
    private String notes;
    
    @Column(length = 2000)
    private String prescription;
    
    @Column(length = 1000)
    private String cancellationReason;
    
    private LocalDateTime cancelledAt;
    
    private LocalDateTime completedAt;
    
    @Column(nullable = false)
    private Double consultationFee;
    
    @Column(nullable = false)
    private Boolean isPaid = false;
    
    private String paymentId;
    
    @Column(nullable = false)
    private Boolean notificationSent = false;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
