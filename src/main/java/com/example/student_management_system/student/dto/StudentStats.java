package com.example.student_management_system.student.dto;

/** Headline numbers for the Lead dashboard. */
public record StudentStats(
        long totalStudents,
        long profilesComplete,
        long documentsComplete,
        long hostelAllocated,
        long awaitingReview,
        long needsCorrection,
        long fullyVerified
) {}
