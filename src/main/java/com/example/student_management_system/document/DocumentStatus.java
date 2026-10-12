package com.example.student_management_system.document;

/**
 * Review status of an uploaded document. A "missing" document simply has no row.
 */
public enum DocumentStatus {
    UPLOADED,           // uploaded, waiting for the Lead's review
    VERIFIED,           // checked by the Lead
    NEEDS_CORRECTION    // the Lead asked the student to upload it again (see review comment)
}
