package com.example.bookingapp.ModuleAppointment.repository;

import com.example.bookingapp.ModuleAppointment.entity.AppointmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
    
    @Query("SELECT s FROM AppointmentSlot s WHERE s.doctor.id = :doctorId " +
           "AND s.slotStartTime >= :startDate " +
           "AND s.slotStartTime < :endDate " +
           "AND s.isAvailable = true AND s.isBooked = false " +
           "ORDER BY s.slotStartTime")
    List<AppointmentSlot> findAvailableSlotsByDoctorAndDateRange(
        @Param("doctorId") Long doctorId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AppointmentSlot s WHERE s.id = :slotId")
    Optional<AppointmentSlot> findByIdWithLock(@Param("slotId") Long slotId);
    
    @Query("SELECT s FROM AppointmentSlot s WHERE s.doctor.id = :doctorId " +
           "AND s.slotStartTime = :slotStartTime")
    Optional<AppointmentSlot> findByDoctorAndStartTime(
        @Param("doctorId") Long doctorId,
        @Param("slotStartTime") LocalDateTime slotStartTime
    );
    
    List<AppointmentSlot> findByDoctorIdAndIsBookedFalseAndIsAvailableTrue(Long doctorId);
}
