package com.example.bookingapp.ModuleAppointment.dto;

import com.example.bookingapp.ModuleAppointment.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    
    private Long id;
    private Long userId;
    private String userName;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private LocalDateTime appointmentDate;
    private AppointmentStatus status;
    private String symptoms;
    private String notes;
    private String prescription;
    private Double consultationFee;
    private Boolean isPaid;
    private Boolean notificationSent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
