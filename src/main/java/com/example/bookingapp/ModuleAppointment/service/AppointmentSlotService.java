package com.example.bookingapp.ModuleAppointment.service;

import com.example.bookingapp.ModuleAppointment.dto.SlotResponse;
import com.example.bookingapp.ModuleAppointment.entity.AppointmentSlot;
import com.example.bookingapp.ModuleAppointment.entity.Doctor;
import com.example.bookingapp.ModuleAppointment.exception.DoctorNotFoundException;
import com.example.bookingapp.ModuleAppointment.repository.AppointmentSlotRepository;
import com.example.bookingapp.ModuleAppointment.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentSlotService {
    
    private final AppointmentSlotRepository slotRepository;
    private final DoctorRepository doctorRepository;
    
    @Transactional
    public List<SlotResponse> generateSlotsForDoctor(Long doctorId, LocalDate date, 
                                                      LocalTime startTime, LocalTime endTime, 
                                                      int slotDurationMinutes) {
        log.info("Generating slots for doctor: {} on date: {}", doctorId, date);
        
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found with ID: " + doctorId));
        
        List<AppointmentSlot> slots = new ArrayList<>();
        LocalDateTime currentSlotStart = LocalDateTime.of(date, startTime);
        LocalDateTime dayEnd = LocalDateTime.of(date, endTime);
        
        while (currentSlotStart.plusMinutes(slotDurationMinutes).isBefore(dayEnd) || 
               currentSlotStart.plusMinutes(slotDurationMinutes).isEqual(dayEnd)) {
            
            LocalDateTime slotEnd = currentSlotStart.plusMinutes(slotDurationMinutes);
            
            // Check if slot already exists
            if (slotRepository.findByDoctorAndStartTime(doctorId, currentSlotStart).isEmpty()) {
                AppointmentSlot slot = new AppointmentSlot();
                slot.setDoctor(doctor);
                slot.setSlotStartTime(currentSlotStart);
                slot.setSlotEndTime(slotEnd);
                slot.setIsBooked(false);
                slot.setIsAvailable(true);
                slots.add(slot);
            }
            
            currentSlotStart = slotEnd;
        }
        
        List<AppointmentSlot> savedSlots = slotRepository.saveAll(slots);
        log.info("Generated {} slots for doctor: {}", savedSlots.size(), doctorId);
        
        return savedSlots.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<SlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        List<AppointmentSlot> slots = slotRepository.findAvailableSlotsByDoctorAndDateRange(
                doctorId, startOfDay, endOfDay);
        
        return slots.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<SlotResponse> getAvailableSlotsForDateRange(Long doctorId, 
                                                             LocalDateTime startDate, 
                                                             LocalDateTime endDate) {
        List<AppointmentSlot> slots = slotRepository.findAvailableSlotsByDoctorAndDateRange(
                doctorId, startDate, endDate);
        
        return slots.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void markSlotAsBooked(Long slotId) {
        AppointmentSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found with ID: " + slotId));
        
        slot.setIsBooked(true);
        slotRepository.save(slot);
        log.info("Slot marked as booked: {}", slotId);
    }
    
    @Transactional
    public void releaseSlot(Long slotId) {
        AppointmentSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found with ID: " + slotId));
        
        slot.setIsBooked(false);
        slotRepository.save(slot);
        log.info("Slot released: {}", slotId);
    }
    
    private SlotResponse mapToResponse(AppointmentSlot slot) {
        return SlotResponse.builder()
                .id(slot.getId())
                .doctorId(slot.getDoctor().getId())
                .doctorName(slot.getDoctor().getFullName())
                .slotStartTime(slot.getSlotStartTime())
                .slotEndTime(slot.getSlotEndTime())
                .isBooked(slot.getIsBooked())
                .isAvailable(slot.getIsAvailable())
                .build();
    }
}
