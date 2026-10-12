package com.example.student_management_system.appointment.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Body of POST /api/appointments/slots, e.g. {"date":"2026-11-02","startTime":"10:00","endTime":"10:30"} */
public record SlotRequest(
        @NotNull(message = "Date is required")
        LocalDate date,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime
) {}
