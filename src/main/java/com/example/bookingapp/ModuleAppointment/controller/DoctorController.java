package com.example.bookingapp.ModuleAppointment.controller;

import com.example.bookingapp.ModuleAppointment.dto.CreateDoctorRequest;
import com.example.bookingapp.ModuleAppointment.dto.DoctorResponse;
import com.example.bookingapp.ModuleAppointment.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {
    
    private final DoctorService doctorService;
    
    @PostMapping
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        DoctorResponse response = doctorService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{doctorId}")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable Long doctorId) {
        DoctorResponse response = doctorService.getDoctorById(doctorId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        List<DoctorResponse> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(doctors);
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<DoctorResponse>> getAvailableDoctors() {
        List<DoctorResponse> doctors = doctorService.getAvailableDoctors();
        return ResponseEntity.ok(doctors);
    }
    
    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<List<DoctorResponse>> getDoctorsBySpecialization(@PathVariable String specialization) {
        List<DoctorResponse> doctors = doctorService.getDoctorsBySpecialization(specialization);
        return ResponseEntity.ok(doctors);
    }
    
    @GetMapping("/specializations")
    public ResponseEntity<List<String>> getAllSpecializations() {
        List<String> specializations = doctorService.getAllSpecializations();
        return ResponseEntity.ok(specializations);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<DoctorResponse>> searchDoctors(@RequestParam String keyword) {
        List<DoctorResponse> doctors = doctorService.searchDoctors(keyword);
        return ResponseEntity.ok(doctors);
    }
    
    @PatchMapping("/{doctorId}/availability")
    public ResponseEntity<DoctorResponse> updateDoctorAvailability(
            @PathVariable Long doctorId,
            @RequestParam Boolean isAvailable) {
        DoctorResponse response = doctorService.updateDoctorAvailability(doctorId, isAvailable);
        return ResponseEntity.ok(response);
    }
}
