package com.example.student_management_system.appointment.dto;

import com.example.student_management_system.appointment.AppointmentStatus;
import com.example.student_management_system.user.Role;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** A booking, as shown in the student's history or the Lead's booking overview. */
public record AppointmentResponse(
        Long id,
        AppointmentStatus status,
        Instant bookedAt,
        Instant cancelledAt,
        Role cancelledBy,
        Long slotId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        boolean upcoming,
        boolean cancellable,
        Long leadId,
        String leadName,
        Long studentId,
        String studentName,
        String studentEmail
) {}
