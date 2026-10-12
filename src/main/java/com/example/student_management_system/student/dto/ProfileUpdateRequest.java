package com.example.student_management_system.student.dto;

import com.example.student_management_system.student.Gender;
import com.example.student_management_system.student.StudentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body of PUT /api/students/{id}/profile.
 * Fields other than fullName may be left empty: the student can save a partial
 * profile and finish it later (completion is tracked separately).
 * Email, role, account id and review status are NOT here, so they can't be changed this way.
 */
public record ProfileUpdateRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @Pattern(regexp = "^$|^[+0-9 ()-]{6,30}$", message = "Phone may contain digits, spaces, +, ( ) and - (6-30 characters)")
        String phone,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        Gender gender,

        @Size(max = 60, message = "Nationality must be at most 60 characters")
        String nationality,

        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,

        @Size(max = 255, message = "Emergency contact must be at most 255 characters")
        String emergencyContact,

        @Pattern(regexp = "^$|^[A-Za-z0-9]{5,20}$", message = "Passport number may contain letters and digits only (5-20 characters)")
        String passportNumber,

        LocalDate passportExpiry,

        LocalDate visaExpiry,

        LocalDate arrivalDate,

        LocalDate departureDate,

        @Size(max = 150, message = "Home university must be at most 150 characters")
        String homeUniversity,

        @Pattern(regexp = "^$|^[A-Za-z0-9-]{3,30}$", message = "Roll number may contain letters, digits and - (3-30 characters)")
        String studentNumber,

        @Size(max = 120, message = "Programme must be at most 120 characters")
        String programme,

        @Size(max = 120, message = "Department must be at most 120 characters")
        String department,

        @Size(max = 40, message = "Semester must be at most 40 characters")
        String academicYear,

        StudentCategory studentCategory
) {}
