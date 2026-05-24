package com.example.bookingapp.ModuleAppointment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAppointmentRequest {
    
    @NotNull(message = "Doctor ID is required")
    private Long doctorId;
    
    @NotNull(message = "Slot ID is required")
    private Long slotId;
    
    private String symptoms;
    
    private String notes;
}
