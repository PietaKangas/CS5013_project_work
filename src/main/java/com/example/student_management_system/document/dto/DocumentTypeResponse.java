package com.example.student_management_system.document.dto;

import com.example.student_management_system.document.DocumentType;
import com.example.student_management_system.student.StudentCategory;

public record DocumentTypeResponse(
        Long id,
        String code,
        String name,
        String description,
        StudentCategory studentCategory,
        boolean active,
        int sortOrder,
        String formUrl
) {
    public static DocumentTypeResponse from(DocumentType t) {
        return new DocumentTypeResponse(t.getId(), t.getCode(), t.getName(), t.getDescription(),
                t.getStudentCategory(), t.isActive(), t.getSortOrder(), t.getFormUrl());
    }
}
