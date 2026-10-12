package com.example.student_management_system.student;

import com.example.student_management_system.common.ApiException;
import com.example.student_management_system.security.AuthUser;
import com.example.student_management_system.user.AppUser;
import com.example.student_management_system.user.Role;
import com.example.student_management_system.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Ownership checks shared by the profile and document modules.
 * This is what stops a student from reading someone else's data just by
 * changing the id in the URL.
 */
@Component
public class StudentAccess {

    private final UserRepository userRepository;

    public StudentAccess(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Leads may view any student; a student may only view themself. Returns the student account. */
    public AppUser requireViewable(AuthUser me, Long studentId) {
        if (me.getRole() != Role.LEAD && !me.getId().equals(studentId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only access your own information");
        }
        return requireStudent(studentId);
    }

    /** Only the student themself may change their data (Leads have read-only access). */
    public AppUser requireSelf(AuthUser me, Long studentId) {
        if (me.getRole() != Role.STUDENT || !me.getId().equals(studentId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only change your own information");
        }
        return requireStudent(studentId);
    }

    /** The student themself, or a Lead (used for hostel details, which the Lead may correct). */
    public AppUser requireSelfOrLead(AuthUser me, Long studentId) {
        if (me.getRole() != Role.LEAD && !me.getId().equals(studentId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only change your own information");
        }
        return requireStudent(studentId);
    }

    public AppUser requireStudent(Long studentId) {
        return userRepository.findByIdAndRole(studentId, Role.STUDENT)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));
    }
}
