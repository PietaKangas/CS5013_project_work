package com.example.student_management_system.document.dto;

import com.example.student_management_system.student.StudentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for creating (POST) or editing (PUT) a document type.
 * "code" is only used when creating; it can't be changed afterwards.
 * studentCategory null = required for all students.
 */
public record DocumentTypeRequest(
        @Pattern(regexp = "^$|^[A-Z0-9_]{2,50}$", message = "Code must be 2-50 capital letters, digits or _")
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        StudentCategory studentCategory,

        Boolean active,

        Integer sortOrder,

        @Pattern(regexp = "^$|^https?://\\S{3,490}$", message = "Form link must be a web address starting with http:// or https://")
        String formUrl
) {}
