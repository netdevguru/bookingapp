package com.example.bookingapp.ModuleAppointment.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    
    private String eventId;
    private String notificationType; // EMAIL, SMS, PUSH
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String message;
    private Long appointmentId;
    private LocalDateTime eventTimestamp;
}
