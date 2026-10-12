package com.example.student_management_system.student.dto;

import com.example.student_management_system.student.HostelStatus;
import com.example.student_management_system.student.ReviewStatus;
import com.example.student_management_system.student.StudentCategory;

import java.util.List;

/**
 * One row of the Lead dashboard / student list.
 * reviewState is a one-word summary for the Lead:
 *   NEEDS_CORRECTION  something was sent back to the student and isn't fixed yet
 *   AWAITING_REVIEW   the student submitted something the Lead hasn't checked
 *   INCOMPLETE        nothing to review, but profile or documents are still missing
 *   VERIFIED          profile, hostel and all required documents verified
 */
public record StudentSummary(
        Long id,
        String fullName,
        String email,
        String studentNumber,
        String nationality,
        String homeUniversity,
        String programme,
        String department,
        StudentCategory studentCategory,
        HostelStatus hostelStatus,
        String hostelName,
        String roomNumber,
        boolean profileComplete,
        int profileCompletionPercent,
        ReviewStatus profileStatus,
        ReviewStatus hostelReviewStatus,
        int documentsUploaded,
        int documentsRequired,
        int documentsVerified,
        boolean documentsComplete,
        int documentCompletionPercent,
        List<String> missingDocuments,
        List<Long> missingDocumentTypeIds,
        List<String> documentsNeedingCorrection,
        int documentsAwaitingReview,
        String reviewState
) {}
