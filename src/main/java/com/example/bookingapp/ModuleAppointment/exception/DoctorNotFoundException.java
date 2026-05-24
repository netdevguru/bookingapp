package com.example.bookingapp.ModuleAppointment.exception;

public class DoctorNotFoundException extends RuntimeException {
    
    public DoctorNotFoundException(String message) {
        super(message);
    }
}
