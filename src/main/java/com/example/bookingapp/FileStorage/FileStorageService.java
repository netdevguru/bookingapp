package com.example.bookingapp.FileStorage;

import com.example.bookingapp.ModuleUser.InvalidImageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;


@Service
public class FileStorageService {
    private final Path fileStorageLocation;
    private final Path profileImageLocation;
    private static final long MAX_PROFILE_IMAGE_SIZE = 1048576; // 1MB in bytes
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png");
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        this.profileImageLocation = this.fileStorageLocation.resolve("profile-images").normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            Files.createDirectories(this.profileImageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create upload directory", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Validate filename
            if (originalFileName.contains("..")) {
                throw new FileStorageException("Invalid file path: " + originalFileName);
            }
            
            if (originalFileName == null || originalFileName.isEmpty()) {
                throw new FileStorageException("Filename cannot be empty");
            }

            // Generate timestamped filename
            String timestampedFileName = generateTimestampedFileName(originalFileName);

            // Store file with new name
            Path targetLocation = this.fileStorageLocation.resolve(timestampedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return timestampedFileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + originalFileName, ex);
        }
    }
    
    /**
     * Generate a timestamped filename
     * Format: originalName_yyyyMMdd_HHmmss_SSS.extension
     * Example: document.pdf -> document_20241219_143025_123.pdf
     */
    private String generateTimestampedFileName(String originalFileName) {
        // Get current timestamp
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
        String timestamp = now.format(formatter);
        
        // Split filename and extension
        int lastDotIndex = originalFileName.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < originalFileName.length() - 1) {
            // File has an extension
            String nameWithoutExtension = originalFileName.substring(0, lastDotIndex);
            String extension = originalFileName.substring(lastDotIndex);
            
            // Clean the name (remove special characters except underscore and hyphen)
            nameWithoutExtension = nameWithoutExtension.replaceAll("[^a-zA-Z0-9_-]", "_");
            
            return nameWithoutExtension + "_" + timestamp + extension;
        } else {
            // File has no extension
            String cleanName = originalFileName.replaceAll("[^a-zA-Z0-9_-]", "_");
            return cleanName + "_" + timestamp;
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileNotFoundException("File not found: " + fileName, ex);
        }
    }

    // Profile Image Methods

    /**
     * Validate image file for profile upload
     * Only allows JPG and PNG files up to 1MB
     */
    public void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidImageException("File is empty");
        }

        // Check file size (1MB = 1048576 bytes)
        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new InvalidImageException("File size exceeds maximum limit of 1MB");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidImageException("Only JPG and PNG images are allowed");
        }

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new InvalidImageException("Filename cannot be empty");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new InvalidImageException("Only JPG and PNG file extensions are allowed");
        }
    }

    /**
     * Store profile image with user-specific naming
     * Format: user_{userId}_profile.{extension}
     */
    public String storeProfileImage(Long userId, MultipartFile file) {
        validateImageFile(file);

        try {
            String extension = getFileExtension(file.getOriginalFilename());
            String fileName = "user_" + userId + "_profile." + extension;

            // Delete old profile image if exists (any extension)
            deleteOldProfileImage(userId);

            // Store new file
            Path targetLocation = this.profileImageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store profile image", ex);
        }
    }

    /**
     * Delete old profile image for a user (any extension)
     */
    private void deleteOldProfileImage(Long userId) {
        for (String ext : ALLOWED_EXTENSIONS) {
            String fileName = "user_" + userId + "_profile." + ext;
            Path filePath = this.profileImageLocation.resolve(fileName);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ex) {
                // Log but don't throw - continue with upload
            }
        }
    }

    /**
     * Delete profile image for a user
     */
    public void deleteProfileImage(Long userId) {
        deleteOldProfileImage(userId);
    }

    /**
     * Get profile image filename if exists
     */
    public String getProfileImageFileName(Long userId) {
        for (String ext : ALLOWED_EXTENSIONS) {
            String fileName = "user_" + userId + "_profile." + ext;
            Path filePath = this.profileImageLocation.resolve(fileName);
            if (Files.exists(filePath)) {
                return fileName;
            }
        }
        return null;
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}