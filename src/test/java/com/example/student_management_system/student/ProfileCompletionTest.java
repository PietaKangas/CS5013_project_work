package com.example.student_management_system.student;

import com.example.student_management_system.common.PageResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain unit tests (no database needed). */
class ProfileCompletionTest {

    @Test
    void emptyProfileIsIncomplete() {
        ProfileCompletion c = ProfileCompletion.of("Asha Rao", null);
        assertFalse(c.complete());
        assertEquals(7, c.percent());                  // only the name (1 of 15 fields)
        assertTrue(c.missingFields().contains("Phone number"));
    }

    @Test
    void fullyFilledProfileIsComplete() {
        StudentProfile p = new StudentProfile(1L);
        p.setPhone("+91 98765 43210");
        p.setDateOfBirth(LocalDate.of(2002, 5, 1));
        p.setGender(Gender.FEMALE);
        p.setPassportNumber("X1234567");
        p.setPassportExpiry(LocalDate.of(2031, 1, 1));
        p.setArrivalDate(LocalDate.of(2026, 7, 20));
        p.setHomeUniversity("Aalto University");
        p.setNationality("Indian");
        p.setAddress("12 Campus Road");
        p.setStudentNumber("S12345");
        p.setProgramme("MSc Computer Science");
        p.setDepartment("Computing");
        p.setAcademicYear("Year 1 / Semester 1");
        p.setStudentCategory(StudentCategory.DEGREE);

        ProfileCompletion c = ProfileCompletion.of("Asha Rao", p);
        assertTrue(c.complete());
        assertEquals(100, c.percent());
        assertTrue(c.missingFields().isEmpty());
    }

    @Test
    void changingDetailsSendsProfileBackForReview() {
        StudentProfile p = new StudentProfile(1L);
        p.reviewProfile(ReviewStatus.VERIFIED, null, 99L);
        int before = p.profileFingerprint();
        p.setPhone("+91 90000 00000");
        assertTrue(p.profileFingerprint() != before);
        p.markProfileChanged();
        assertEquals(ReviewStatus.PENDING, p.getProfileStatus());
    }

    @Test
    void pagingSplitsResults() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        PageResponse<Integer> second = PageResponse.of(numbers, 1, 2);
        assertEquals(List.of(3, 4), second.content());
        assertEquals(3, second.totalPages());
        assertEquals(5, second.totalElements());
        assertTrue(PageResponse.of(numbers, 9, 2).content().isEmpty());
    }
}
