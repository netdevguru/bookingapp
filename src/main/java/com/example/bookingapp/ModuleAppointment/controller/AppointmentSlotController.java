package com.example.bookingapp.ModuleAppointment.controller;

import com.example.bookingapp.ModuleAppointment.dto.SlotResponse;
import com.example.bookingapp.ModuleAppointment.service.AppointmentSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class AppointmentSlotController {
    private final AppointmentSlotService slotService;
    
    @PostMapping("/generate")
    public ResponseEntity<List<SlotResponse>> generateSlots(
        @RequestParam Long doctorId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
        @RequestParam(defaultValue = "30") int slotDurationMinutes
    ) {
        
        List<SlotResponse> slots = slotService.generateSlotsForDoctor(doctorId, date, startTime, endTime, slotDurationMinutes);
        return ResponseEntity.status(HttpStatus.CREATED).body(slots);
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<SlotResponse>> getAvailableSlots(
        @RequestParam Long doctorId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<SlotResponse> slots = slotService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(slots);
    }
    
    @GetMapping("/available/range")
    public ResponseEntity<List<SlotResponse>> getAvailableSlotsForDateRange(
        @RequestParam Long doctorId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<SlotResponse> slots = slotService.getAvailableSlotsForDateRange(doctorId, startDate, endDate);
        return ResponseEntity.ok(slots);
    }
}
