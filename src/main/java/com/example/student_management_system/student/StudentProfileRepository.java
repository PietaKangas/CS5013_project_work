package com.example.student_management_system.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    Optional<StudentProfile> findByStudentNumberIgnoreCase(String studentNumber);

    /** True if another student (not userId) already uses this student number. */
    boolean existsByStudentNumberIgnoreCaseAndUserIdNot(String studentNumber, Long userId);
}
