package com.example.student_management_system.document.dto;

/** One line of the checklist: a required document type, an optional form link, and the uploaded file (null = missing). */
public record ChecklistItem(
        Long documentTypeId,
        String code,
        String name,
        String description,
        String formUrl,
        boolean uploaded,
        DocumentResponse document
) {}
