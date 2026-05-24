package com.example.bookingapp.ModuleAppointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotResponse {
    
    private Long id;
    private Long doctorId;
    private String doctorName;
    private LocalDateTime slotStartTime;
    private LocalDateTime slotEndTime;
    private Boolean isBooked;
    private Boolean isAvailable;
}
