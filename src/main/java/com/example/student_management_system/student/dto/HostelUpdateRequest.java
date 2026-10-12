package com.example.student_management_system.student.dto;

import com.example.student_management_system.student.HostelStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of PUT /api/students/{id}/hostel. */
public record HostelUpdateRequest(
        @Size(max = 120, message = "Hostel name must be at most 120 characters")
        String hostelName,

        @Size(max = 20, message = "Room number must be at most 20 characters")
        String roomNumber,

        @NotNull(message = "Hostel allocation status is required")
        HostelStatus hostelStatus
) {}
