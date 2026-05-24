package com.example.bookingapp.FileStorage.exception;

import com.example.bookingapp.ModuleUser.exception.InvalidImageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class FileUploadExceptionAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("File size exceeds maximum limit!");
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<String> handleFileStorageException(FileStorageException exc) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exc.getMessage());
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<String> handleFileNotFoundException(FileNotFoundException exc) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exc.getMessage());
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidImageException(InvalidImageException exc) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", exc.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}