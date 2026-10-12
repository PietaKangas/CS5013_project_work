package com.example.student_management_system.auth.dto;

import com.example.student_management_system.user.AppUser;
import com.example.student_management_system.user.Role;

/** Safe public view of an account: never includes the password hash. */
public record UserResponse(Long id, String fullName, String email, Role role) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
