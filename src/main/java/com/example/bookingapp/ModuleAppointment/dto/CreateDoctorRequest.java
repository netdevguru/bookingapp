package com.example.bookingapp.ModuleAppointment.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateDoctorRequest {
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Specialization is required")
    private String specialization;
    
    private String qualifications;
    
    @NotNull(message = "Experience years is required")
    @Min(value = 0, message = "Experience years must be positive")
    private Integer experienceYears;
    
    private String phoneNumber;
    
    private String bio;
    
    @NotNull(message = "Consultation fee is required")
    @Positive(message = "Consultation fee must be positive")
    private Double consultationFee;
}
