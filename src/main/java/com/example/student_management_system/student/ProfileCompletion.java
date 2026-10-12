package com.example.student_management_system.student;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out whether a profile is complete from the actual field values
 * (never stored by hand, so it can't get out of date).
 * Hostel fields are not part of this: they're tracked by hostelStatus instead.
 * Optional fields (emergency contact, visa expiry, departure date) are not counted.
 */
public record ProfileCompletion(boolean complete, int percent, List<String> missingFields) {

    public static ProfileCompletion of(String fullName, StudentProfile p) {
        List<String> missing = new ArrayList<>();
        check(missing, !isBlank(fullName), "Full name");
        check(missing, p != null && !isBlank(p.getPhone()), "Phone number");
        check(missing, p != null && p.getDateOfBirth() != null, "Date of birth");
        check(missing, p != null && p.getGender() != null, "Gender");
        check(missing, p != null && !isBlank(p.getNationality()), "Nationality");
        check(missing, p != null && !isBlank(p.getAddress()), "Address");
        check(missing, p != null && !isBlank(p.getPassportNumber()), "Passport number");
        check(missing, p != null && p.getPassportExpiry() != null, "Passport expiry date");
        check(missing, p != null && p.getArrivalDate() != null, "Arrival date");
        check(missing, p != null && !isBlank(p.getHomeUniversity()), "Home university");
        check(missing, p != null && !isBlank(p.getStudentNumber()), "IIT Madras roll number");
        check(missing, p != null && !isBlank(p.getProgramme()), "Programme");
        check(missing, p != null && !isBlank(p.getDepartment()), "Department");
        check(missing, p != null && !isBlank(p.getAcademicYear()), "Semester");
        check(missing, p != null && p.getStudentCategory() != null, "Student category");

        int total = 15;
        int filled = total - missing.size();
        int percent = Math.round(filled * 100f / total);
        return new ProfileCompletion(missing.isEmpty(), percent, List.copyOf(missing));
    }

    private static void check(List<String> missing, boolean ok, String label) {
        if (!ok) missing.add(label);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
