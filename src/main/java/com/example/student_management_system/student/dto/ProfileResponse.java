package com.example.student_management_system.student.dto;

import com.example.student_management_system.student.Gender;
import com.example.student_management_system.student.HostelStatus;
import com.example.student_management_system.student.ProfileCompletion;
import com.example.student_management_system.student.ReviewStatus;
import com.example.student_management_system.student.StudentCategory;
import com.example.student_management_system.student.StudentProfile;
import com.example.student_management_system.user.AppUser;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Full profile as returned to the student themself or to a Lead, including the review status. */
public record ProfileResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        Gender gender,
        String nationality,
        String address,
        String emergencyContact,
        String passportNumber,
        LocalDate passportExpiry,
        LocalDate visaExpiry,
        LocalDate arrivalDate,
        LocalDate departureDate,
        String homeUniversity,
        String studentNumber,
        String programme,
        String department,
        String academicYear,
        StudentCategory studentCategory,
        String hostelName,
        String roomNumber,
        HostelStatus hostelStatus,
        boolean profileComplete,
        int profileCompletionPercent,
        List<String> missingFields,
        ReviewStatus profileStatus,
        String profileComment,
        Instant profileReviewedAt,
        ReviewStatus hostelReviewStatus,
        String hostelComment,
        Instant hostelReviewedAt,
        Instant updatedAt
) {
    /** p may be null when the student hasn't saved a profile yet. */
    public static ProfileResponse from(AppUser user, StudentProfile p) {
        ProfileCompletion c = ProfileCompletion.of(user.getFullName(), p);
        boolean none = p == null;
        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                none ? null : p.getPhone(),
                none ? null : p.getDateOfBirth(),
                none ? null : p.getGender(),
                none ? null : p.getNationality(),
                none ? null : p.getAddress(),
                none ? null : p.getEmergencyContact(),
                none ? null : p.getPassportNumber(),
                none ? null : p.getPassportExpiry(),
                none ? null : p.getVisaExpiry(),
                none ? null : p.getArrivalDate(),
                none ? null : p.getDepartureDate(),
                none ? null : p.getHomeUniversity(),
                none ? null : p.getStudentNumber(),
                none ? null : p.getProgramme(),
                none ? null : p.getDepartment(),
                none ? null : p.getAcademicYear(),
                none ? null : p.getStudentCategory(),
                none ? null : p.getHostelName(),
                none ? null : p.getRoomNumber(),
                none ? HostelStatus.NOT_APPLIED : p.getHostelStatus(),
                c.complete(),
                c.percent(),
                c.missingFields(),
                none ? ReviewStatus.PENDING : p.getProfileStatus(),
                none ? null : p.getProfileComment(),
                none ? null : p.getProfileReviewedAt(),
                none ? ReviewStatus.PENDING : p.getHostelReviewStatus(),
                none ? null : p.getHostelComment(),
                none ? null : p.getHostelReviewedAt(),
                none ? null : p.getUpdatedAt());
    }
}
