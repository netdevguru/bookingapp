package com.example.bookingapp.ModuleAppointment.service;

import com.example.bookingapp.ModuleAppointment.dto.AppointmentResponse;
import com.example.bookingapp.ModuleAppointment.dto.CancelAppointmentRequest;
import com.example.bookingapp.ModuleAppointment.dto.CreateAppointmentRequest;
import com.example.bookingapp.ModuleAppointment.entity.Appointment;
import com.example.bookingapp.ModuleAppointment.entity.AppointmentHistory;
import com.example.bookingapp.ModuleAppointment.entity.AppointmentSlot;
import com.example.bookingapp.ModuleAppointment.entity.Doctor;
import com.example.bookingapp.ModuleAppointment.enums.AppointmentStatus;
import com.example.bookingapp.ModuleAppointment.events.AppointmentEvent;
import com.example.bookingapp.ModuleAppointment.events.NotificationEvent;
import com.example.bookingapp.ModuleAppointment.exception.AppointmentException;
import com.example.bookingapp.ModuleAppointment.exception.DoctorNotFoundException;
import com.example.bookingapp.ModuleAppointment.exception.SlotNotAvailableException;
import com.example.bookingapp.ModuleAppointment.repository.AppointmentHistoryRepository;
import com.example.bookingapp.ModuleAppointment.repository.AppointmentRepository;
import com.example.bookingapp.ModuleAppointment.repository.AppointmentSlotRepository;
import com.example.bookingapp.ModuleAppointment.repository.DoctorRepository;
import com.example.bookingapp.ModuleUser.UserEntity;
import com.example.bookingapp.ModuleUser.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {
    
    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlotRepository slotRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final AppointmentHistoryRepository historyRepository;
    private final EventPublisherService eventPublisher;
    
    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request, String userEmail) {
        log.info("Creating appointment for user: {} with doctor: {}", userEmail, request.getDoctorId());
        
        // Get user
        UserEntity user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        // Get doctor
        Doctor doctor = doctorRepository.findById(request.getDoctorId()).orElseThrow(() -> new DoctorNotFoundException("Doctor not found with ID: " + request.getDoctorId()));
        
        // Get and lock slot to prevent concurrent booking
        AppointmentSlot slot = slotRepository.findByIdWithLock(request.getSlotId()).orElseThrow(() -> new SlotNotAvailableException("Slot not found with ID: " + request.getSlotId()));
        
        // Validate slot availability
        if (slot.getIsBooked() || !slot.getIsAvailable()) {
            throw new SlotNotAvailableException("Selected slot is not available");
        }
        
        // Validate slot belongs to the doctor
        if (!slot.getDoctor().getId().equals(request.getDoctorId())) {
            throw new AppointmentException("Slot does not belong to the selected doctor");
        }
        
        // Check for duplicate booking
        if (appointmentRepository.existsActiveAppointmentForSlot(slot.getId())) {
            throw new AppointmentException("This slot is already booked");
        }
        
        // Create appointment
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setDoctor(doctor);
        appointment.setSlot(slot);
        appointment.setAppointmentDate(slot.getSlotStartTime());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setSymptoms(request.getSymptoms());
        appointment.setNotes(request.getNotes());
        appointment.setConsultationFee(doctor.getConsultationFee());
        appointment.setIsPaid(false);
        appointment.setNotificationSent(false);
        
        Appointment savedAppointment = appointmentRepository.save(appointment);
        
        // Mark slot as booked
        slot.setIsBooked(true);
        slotRepository.save(slot);
        
        // Create history entry
        createHistoryEntry(savedAppointment.getId(), null, AppointmentStatus.SCHEDULED, "Appointment created", userEmail);
        
        // Publish Kafka event in a different thread after transaction commits
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture.runAsync(() -> {
                    try {
                        publishAppointmentCreatedEvent(savedAppointment);
                    } catch (Exception e) {
                        log.error("Failed to publish appointment created event asynchronously", e);
                    }
                });
            }
        });
        
        log.info("Appointment created successfully with ID: {}", savedAppointment.getId());
        return mapToResponse(savedAppointment);
    }
    
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, CancelAppointmentRequest request, String userEmail) {
        log.info("Cancelling appointment: {} by user: {}", appointmentId, userEmail);
        
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow(() -> new AppointmentException("Appointment not found with ID: " + appointmentId));
        
        // Validate user owns the appointment
        if (!appointment.getUser().getEmail().equals(userEmail)) {
            throw new AppointmentException("You are not authorized to cancel this appointment");
        }
        
        // Validate appointment can be cancelled
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentException("Appointment is already cancelled");
        }
        
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppointmentException("Cannot cancel a completed appointment");
        }
        
        AppointmentStatus oldStatus = appointment.getStatus();
        
        // Update appointment
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.getCancellationReason());
        appointment.setCancelledAt(LocalDateTime.now());
        
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        
        // Release the slot
        AppointmentSlot slot = appointment.getSlot();
        slot.setIsBooked(false);
        slotRepository.save(slot);
        
        // Create history entry
        createHistoryEntry(appointmentId, oldStatus, AppointmentStatus.CANCELLED, request.getCancellationReason(), userEmail);
        
        // Publish Kafka event in a different thread after transaction commits
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture.runAsync(() -> {
                    try {
                        publishAppointmentCancelledEvent(updatedAppointment);
                    } catch (Exception e) {
                        log.error("Failed to publish appointment cancelled event asynchronously", e);
                    }
                });
            }
        });
        
        log.info("Appointment cancelled successfully: {}", appointmentId);
        return mapToResponse(updatedAppointment);
    }
    
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getUserAppointments(String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        return appointmentRepository.findByUserId(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getUserAppointmentHistory(String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        return appointmentRepository.findUserAppointmentHistory(user.getId(), threeMonthsAgo).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long appointmentId, String userEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentException("Appointment not found with ID: " + appointmentId));
        
        // Validate user owns the appointment
        if (!appointment.getUser().getEmail().equals(userEmail)) {
            throw new AppointmentException("You are not authorized to view this appointment");
        }
        
        return mapToResponse(appointment);
    }
    
    @Transactional(readOnly = true)
    public List<AppointmentHistory> getAppointmentHistory(Long appointmentId) {
        return historyRepository.findByAppointmentIdOrderByChangedAtDesc(appointmentId);
    }
    
    @Transactional
    public AppointmentResponse completeAppointment(Long appointmentId, String prescription) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentException("Appointment not found with ID: " + appointmentId));
        
        AppointmentStatus oldStatus = appointment.getStatus();
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setPrescription(prescription);
        appointment.setCompletedAt(LocalDateTime.now());
        
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        
        createHistoryEntry(appointmentId, oldStatus, AppointmentStatus.COMPLETED, "Appointment completed", "SYSTEM");
        
        // Publish Kafka event in a different thread after transaction commits
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture.runAsync(() -> {
                    try {
                        publishAppointmentCompletedEvent(updatedAppointment);
                    } catch (Exception e) {
                        log.error("Failed to publish appointment completed event asynchronously", e);
                    }
                });
            }
        });
        
        return mapToResponse(updatedAppointment);
    }
    
    private void createHistoryEntry(Long appointmentId, AppointmentStatus oldStatus, 
                                    AppointmentStatus newStatus, String reason, String changedBy) {
        AppointmentHistory history = new AppointmentHistory();
        history.setAppointmentId(appointmentId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangeReason(reason);
        history.setChangedBy(changedBy);
        historyRepository.save(history);
    }
    
    private void publishAppointmentCreatedEvent(Appointment appointment) {
        AppointmentEvent event = AppointmentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CREATED")
                .appointmentId(appointment.getId())
                .userId(appointment.getUser().getId())
                .userEmail(appointment.getUser().getEmail())
                .userName(appointment.getUser().getFirstName() + " " + appointment.getUser().getLastName())
                .doctorId(appointment.getDoctor().getId())
                .doctorName(appointment.getDoctor().getFullName())
                .doctorEmail(appointment.getDoctor().getEmail())
                .doctorSpecialization(appointment.getDoctor().getSpecialization())
                .appointmentDate(appointment.getAppointmentDate())
                .status(appointment.getStatus())
                .symptoms(appointment.getSymptoms())
                .consultationFee(appointment.getConsultationFee())
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishAppointmentEvent(event);
        
        // Publish notification event
        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("EMAIL")
                .recipientEmail(appointment.getUser().getEmail())
                .recipientName(appointment.getUser().getFirstName())
                .subject("Appointment Confirmation")
                .message(String.format("Your appointment with Dr. %s is scheduled for %s", 
                        appointment.getDoctor().getFullName(), 
                        appointment.getAppointmentDate()))
                .appointmentId(appointment.getId())
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishNotificationEvent(notificationEvent);
    }
    
    private void publishAppointmentCancelledEvent(Appointment appointment) {
        AppointmentEvent event = AppointmentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CANCELLED")
                .appointmentId(appointment.getId())
                .userId(appointment.getUser().getId())
                .userEmail(appointment.getUser().getEmail())
                .userName(appointment.getUser().getFirstName() + " " + appointment.getUser().getLastName())
                .doctorId(appointment.getDoctor().getId())
                .doctorName(appointment.getDoctor().getFullName())
                .doctorEmail(appointment.getDoctor().getEmail())
                .appointmentDate(appointment.getAppointmentDate())
                .status(appointment.getStatus())
                .cancellationReason(appointment.getCancellationReason())
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishAppointmentEvent(event);
        
        // Publish notification event
        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("EMAIL")
                .recipientEmail(appointment.getUser().getEmail())
                .recipientName(appointment.getUser().getFirstName())
                .subject("Appointment Cancelled")
                .message(String.format("Your appointment with Dr. %s scheduled for %s has been cancelled", 
                        appointment.getDoctor().getFullName(), 
                        appointment.getAppointmentDate()))
                .appointmentId(appointment.getId())
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishNotificationEvent(notificationEvent);
    }
    
    private void publishAppointmentCompletedEvent(Appointment appointment) {
        AppointmentEvent event = AppointmentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("COMPLETED")
                .appointmentId(appointment.getId())
                .userId(appointment.getUser().getId())
                .userEmail(appointment.getUser().getEmail())
                .userName(appointment.getUser().getFirstName() + " " + appointment.getUser().getLastName())
                .doctorId(appointment.getDoctor().getId())
                .doctorName(appointment.getDoctor().getFullName())
                .appointmentDate(appointment.getAppointmentDate())
                .status(appointment.getStatus())
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishAppointmentEvent(event);
    }
    
    private AppointmentResponse mapToResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .userId(appointment.getUser().getId())
                .userName(appointment.getUser().getFirstName() + " " + appointment.getUser().getLastName())
                .doctorId(appointment.getDoctor().getId())
                .doctorName(appointment.getDoctor().getFullName())
                .doctorSpecialization(appointment.getDoctor().getSpecialization())
                .appointmentDate(appointment.getAppointmentDate())
                .status(appointment.getStatus())
                .symptoms(appointment.getSymptoms())
                .notes(appointment.getNotes())
                .prescription(appointment.getPrescription())
                .consultationFee(appointment.getConsultationFee())
                .isPaid(appointment.getIsPaid())
                .notificationSent(appointment.getNotificationSent())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}
