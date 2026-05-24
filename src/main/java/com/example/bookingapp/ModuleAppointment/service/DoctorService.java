package com.example.bookingapp.ModuleAppointment.service;

import com.example.bookingapp.ModuleAppointment.dto.CreateDoctorRequest;
import com.example.bookingapp.ModuleAppointment.dto.DoctorResponse;
import com.example.bookingapp.ModuleAppointment.entity.Doctor;
import com.example.bookingapp.ModuleAppointment.exception.DoctorNotFoundException;
import com.example.bookingapp.ModuleAppointment.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {
    
    private final DoctorRepository doctorRepository;
    
    @Transactional
    public DoctorResponse createDoctor(CreateDoctorRequest request) {
        log.info("Creating new doctor: {}", request.getEmail());
        
        Doctor doctor = new Doctor();
        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setEmail(request.getEmail());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setQualifications(request.getQualifications());
        doctor.setExperienceYears(request.getExperienceYears());
        doctor.setPhoneNumber(request.getPhoneNumber());
        doctor.setBio(request.getBio());
        doctor.setConsultationFee(request.getConsultationFee());
        doctor.setIsAvailable(true);
        
        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("Doctor created successfully with ID: {}", savedDoctor.getId());
        
        return mapToResponse(savedDoctor);
    }
    
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found with ID: " + doctorId));
        return mapToResponse(doctor);
    }
    
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAvailableDoctors() {
        return doctorRepository.findByIsAvailableTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecialization(specialization).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<String> getAllSpecializations() {
        return doctorRepository.findAllSpecializations();
    }
    
    @Transactional(readOnly = true)
    public List<DoctorResponse> searchDoctors(String keyword) {
        return doctorRepository.searchAvailableDoctors(keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public DoctorResponse updateDoctorAvailability(Long doctorId, Boolean isAvailable) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found with ID: " + doctorId));
        
        doctor.setIsAvailable(isAvailable);
        Doctor updatedDoctor = doctorRepository.save(doctor);
        
        log.info("Doctor availability updated: {} - {}", doctorId, isAvailable);
        return mapToResponse(updatedDoctor);
    }
    
    private DoctorResponse mapToResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .fullName(doctor.getFullName())
                .email(doctor.getEmail())
                .specialization(doctor.getSpecialization())
                .qualifications(doctor.getQualifications())
                .experienceYears(doctor.getExperienceYears())
                .phoneNumber(doctor.getPhoneNumber())
                .bio(doctor.getBio())
                .profileImageUrl(doctor.getProfileImageUrl())
                .isAvailable(doctor.getIsAvailable())
                .consultationFee(doctor.getConsultationFee())
                .build();
    }
}
