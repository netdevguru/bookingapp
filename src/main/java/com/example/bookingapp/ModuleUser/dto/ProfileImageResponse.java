package com.example.bookingapp.ModuleUser.dto;

import java.time.LocalDateTime;

public class ProfileImageResponse {
    private String imageUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadedAt;

    public ProfileImageResponse(String imageUrl, String fileName, Long fileSize, String fileType, LocalDateTime uploadedAt) {
        this.imageUrl = imageUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.uploadedAt = uploadedAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
