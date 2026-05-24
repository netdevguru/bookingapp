package com.example.bookingapp.ModuleAppointment.repository;

import com.example.bookingapp.ModuleAppointment.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    
    Optional<Doctor> findByEmail(String email);
    
    List<Doctor> findByIsAvailableTrue();
    
    List<Doctor> findBySpecialization(String specialization);
    
    @Query("SELECT DISTINCT d.specialization FROM Doctor d ORDER BY d.specialization")
    List<String> findAllSpecializations();
    
    @Query("SELECT d FROM Doctor d WHERE d.isAvailable = true " +
           "AND LOWER(d.specialization) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Doctor> searchAvailableDoctors(@Param("keyword") String keyword);
}
