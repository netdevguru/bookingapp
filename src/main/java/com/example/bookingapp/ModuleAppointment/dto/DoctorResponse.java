package com.example.bookingapp.ModuleAppointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponse {
    
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String specialization;
    private String qualifications;
    private Integer experienceYears;
    private String phoneNumber;
    private String bio;
    private String profileImageUrl;
    private Boolean isAvailable;
    private Double consultationFee;
}
