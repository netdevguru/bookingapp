package com.example.bookingapp.ModuleAppointment.entity;

import com.example.bookingapp.ModuleAppointment.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long appointmentId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus oldStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus newStatus;
    
    @Column(length = 2000)
    private String changeReason;
    
    @Column(nullable = false)
    private String changedBy;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
}
