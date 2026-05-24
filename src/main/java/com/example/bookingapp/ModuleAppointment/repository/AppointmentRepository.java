package com.example.bookingapp.ModuleAppointment.repository;

import com.example.bookingapp.ModuleAppointment.entity.Appointment;
import com.example.bookingapp.ModuleAppointment.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    
    List<Appointment> findByUserId(Long userId);
    
    List<Appointment> findByDoctorId(Long doctorId);
    
    List<Appointment> findByUserIdAndStatus(Long userId, AppointmentStatus status);
    
    List<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);
    
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentDate BETWEEN :startDate AND :endDate " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW')")
    List<Appointment> findDoctorAppointmentsBetween(
        @Param("doctorId") Long doctorId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT a FROM Appointment a WHERE a.user.id = :userId " +
           "AND a.appointmentDate >= :fromDate " +
           "ORDER BY a.appointmentDate DESC")
    List<Appointment> findUserAppointmentHistory(
        @Param("userId") Long userId,
        @Param("fromDate") LocalDateTime fromDate
    );
    
    Optional<Appointment> findBySlotId(Long slotId);
    
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.slot.id = :slotId " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW')")
    boolean existsActiveAppointmentForSlot(@Param("slotId") Long slotId);
}
