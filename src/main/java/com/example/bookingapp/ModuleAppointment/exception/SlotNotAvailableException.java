package com.example.bookingapp.ModuleAppointment.exception;

public class SlotNotAvailableException extends RuntimeException {
    
    public SlotNotAvailableException(String message) {
        super(message);
    }
}
