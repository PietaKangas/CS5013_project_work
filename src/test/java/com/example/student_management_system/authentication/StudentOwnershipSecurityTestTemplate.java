package com.example.student_management_system.authentication;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("No student-specific endpoints exist yet. Add these checks once a student API is introduced.")
class StudentOwnershipSecurityTestTemplate {

    @Test
    void studentA_cannotReadStudentB_data() {
        // Example: GET /api/student/{studentBId}
        // authenticated as studentA
        // expected: 403 or 404
    }

    @Test
    void studentA_cannotModifyStudentB_data() {
        // Example: PUT/PATCH/DELETE /api/student/{studentBId}
        // authenticated as studentA
        // expected: 403 or 404
    }
}
