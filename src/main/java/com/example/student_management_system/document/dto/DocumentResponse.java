package com.example.student_management_system.document.dto;

import com.example.student_management_system.document.DocumentStatus;
import com.example.student_management_system.document.StudentDocument;

import java.time.Instant;

/** Document metadata sent to the browser. The stored file name/path is deliberately left out. */
public record DocumentResponse(
        Long id,
        Long studentId,
        Long documentTypeId,
        String documentTypeName,
        String originalFilename,
        String contentType,
        long sizeBytes,
        DocumentStatus status,
        Instant uploadedAt,
        String reviewComment,
        Instant reviewedAt
) {
    public static DocumentResponse from(StudentDocument d) {
        return new DocumentResponse(
                d.getId(),
                d.getStudentId(),
                d.getDocumentType().getId(),
                d.getDocumentType().getName(),
                d.getOriginalFilename(),
                d.getContentType(),
                d.getSizeBytes(),
                d.getStatus(),
                d.getUploadedAt(),
                d.getReviewComment(),
                d.getReviewedAt());
    }
}
