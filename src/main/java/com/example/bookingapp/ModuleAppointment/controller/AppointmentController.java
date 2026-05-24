package com.example.bookingapp.ModuleAppointment.controller;

import com.example.bookingapp.ModuleAppointment.dto.AppointmentResponse;
import com.example.bookingapp.ModuleAppointment.dto.CancelAppointmentRequest;
import com.example.bookingapp.ModuleAppointment.dto.CreateAppointmentRequest;
import com.example.bookingapp.ModuleAppointment.entity.AppointmentHistory;
import com.example.bookingapp.ModuleAppointment.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    
    private final AppointmentService appointmentService;
    
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        AppointmentResponse response = appointmentService.createAppointment(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getUserAppointments(Authentication authentication) {
        String userEmail = authentication.getName();
        List<AppointmentResponse> appointments = appointmentService.getUserAppointments(userEmail);
        return ResponseEntity.ok(appointments);
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<AppointmentResponse>> getUserAppointmentHistory(Authentication authentication) {
        String userEmail = authentication.getName();
        List<AppointmentResponse> appointments = appointmentService.getUserAppointmentHistory(userEmail);
        return ResponseEntity.ok(appointments);
    }
    
    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @PathVariable Long appointmentId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        AppointmentResponse response = appointmentService.getAppointmentById(appointmentId, userEmail);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CancelAppointmentRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        AppointmentResponse response = appointmentService.cancelAppointment(appointmentId, request, userEmail);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{appointmentId}/history")
    public ResponseEntity<List<AppointmentHistory>> getAppointmentHistory(@PathVariable Long appointmentId) {
        List<AppointmentHistory> history = appointmentService.getAppointmentHistory(appointmentId);
        return ResponseEntity.ok(history);
    }
    
    @PostMapping("/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @PathVariable Long appointmentId,
            @RequestParam(required = false) String prescription) {
        
        AppointmentResponse response = appointmentService.completeAppointment(appointmentId, prescription);
        return ResponseEntity.ok(response);
    }
}
