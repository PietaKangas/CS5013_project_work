package com.example.student_management_system.document.dto;

import com.example.student_management_system.student.StudentCategory;

import java.util.List;

/** A student's full checklist with completion and review progress calculated from the real uploads. */
public record DocumentChecklist(
        Long studentId,
        String studentName,
        StudentCategory studentCategory,
        int requiredCount,
        int uploadedCount,
        int verifiedCount,
        int needsCorrectionCount,
        boolean complete,
        boolean allVerified,
        int completionPercent,
        List<ChecklistItem> items
) {}
