package com.example.bookingapp.ModuleAppointment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_slots", uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "slot_start_time"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSlot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;
    
    @Column(nullable = false)
    private LocalDateTime slotStartTime;
    
    @Column(nullable = false)
    private LocalDateTime slotEndTime;
    
    @Column(nullable = false)
    private Boolean isBooked = false;
    
    @Column(nullable = false)
    private Boolean isAvailable = true;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
