package com.example.student_management_system.student;

/** Result of the International Lead's review of a profile or of the hostel details. */
public enum ReviewStatus {
    PENDING,            // waiting for the Lead (new, or changed by the student since the last review)
    VERIFIED,           // checked and correct
    NEEDS_CORRECTION    // the student must fix something; see the Lead's comment
}
