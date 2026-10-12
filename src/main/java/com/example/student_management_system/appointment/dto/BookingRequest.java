package com.example.student_management_system.appointment.dto;

import jakarta.validation.constraints.NotNull;

/** Body of POST /api/appointments, e.g. {"slotId": 12} */
public record BookingRequest(
        @NotNull(message = "slotId is required")
        Long slotId
) {}
