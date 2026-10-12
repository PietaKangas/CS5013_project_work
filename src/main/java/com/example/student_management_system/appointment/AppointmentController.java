package com.example.student_management_system.appointment;

import com.example.student_management_system.appointment.dto.AppointmentResponse;
import com.example.student_management_system.appointment.dto.BookingRequest;
import com.example.student_management_system.appointment.dto.SlotRequest;
import com.example.student_management_system.appointment.dto.SlotResponse;
import com.example.student_management_system.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Student:
 *   GET  /api/appointments/available      free future slots
 *   POST /api/appointments                book a slot  {"slotId": 1}
 *   GET  /api/appointments/my             my bookings (upcoming + history)
 * Student (own booking) or Lead (booking on own slot):
 *   POST /api/appointments/{id}/cancel    cancel a booking; the slot becomes available again
 * Lead:
 *   POST /api/appointments/slots          create a slot
 *   GET  /api/appointments/slots          my slots (?upcomingOnly=true)
 *   POST /api/appointments/slots/{id}/cancel   cancel a slot (and its booking, if any)
 *   GET  /api/appointments/bookings       all bookings on my slots
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // ----- student -----

    @GetMapping("/available")
    @PreAuthorize("hasRole('STUDENT')")
    public List<SlotResponse> available() {
        return appointmentService.availableSlots();
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody BookingRequest request) {
        return appointmentService.book(me, request.slotId());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<AppointmentResponse> my(@AuthenticationPrincipal AuthUser me) {
        return appointmentService.myAppointments(me);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancelBooking(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return appointmentService.cancelBooking(me, id);
    }

    // ----- lead -----

    @PostMapping("/slots")
    @PreAuthorize("hasRole('LEAD')")
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse createSlot(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody SlotRequest request) {
        return appointmentService.createSlot(me, request);
    }

    @GetMapping("/slots")
    @PreAuthorize("hasRole('LEAD')")
    public List<SlotResponse> slots(@AuthenticationPrincipal AuthUser me,
                                    @RequestParam(defaultValue = "false") boolean upcomingOnly) {
        return appointmentService.leadSlots(me, upcomingOnly);
    }

    @PostMapping("/slots/{id}/cancel")
    @PreAuthorize("hasRole('LEAD')")
    public SlotResponse cancelSlot(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return appointmentService.cancelSlot(me, id);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('LEAD')")
    public List<AppointmentResponse> bookings(@AuthenticationPrincipal AuthUser me) {
        return appointmentService.leadBookings(me);
    }
}
