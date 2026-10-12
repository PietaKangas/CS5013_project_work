package com.example.student_management_system.appointment.dto;

import com.example.student_management_system.appointment.SlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A slot. The booking fields (appointmentId, student...) are only filled in for the
 * Lead's own view of a BOOKED slot; students never see who else booked what.
 */
public record SlotResponse(
        Long id,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        SlotStatus status,
        boolean past,
        Long leadId,
        String leadName,
        Long appointmentId,
        Long studentId,
        String studentName,
        String studentEmail
) {}
