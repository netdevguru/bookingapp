package com.example.bookingapp.ModuleAppointment.events;

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
public class AppointmentEvent {
    
    private String eventId;
    private String eventType; // CREATED, CANCELLED, COMPLETED, RESCHEDULED
    private Long appointmentId;
    private Long userId;
    private String userEmail;
    private String userName;
    private Long doctorId;
    private String doctorName;
    private String doctorEmail;
    private String doctorSpecialization;
    private LocalDateTime appointmentDate;
    private AppointmentStatus status;
    private String symptoms;
    private Double consultationFee;
    private String cancellationReason;
    private LocalDateTime eventTimestamp;
}
