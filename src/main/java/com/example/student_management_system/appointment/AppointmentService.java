package com.example.student_management_system.appointment;

import com.example.student_management_system.appointment.dto.AppointmentResponse;
import com.example.student_management_system.appointment.dto.SlotRequest;
import com.example.student_management_system.appointment.dto.SlotResponse;
import com.example.student_management_system.common.ApiException;
import com.example.student_management_system.security.AuthUser;
import com.example.student_management_system.user.AppUser;
import com.example.student_management_system.user.Role;
import com.example.student_management_system.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Appointment rules:
 *  - Leads create slots (no past slots, no overlapping slots for the same Lead).
 *  - Students book AVAILABLE future slots. Booking is a single atomic SQL UPDATE, so
 *    if two students click "Book" at the same moment only one of them gets the slot.
 *  - Students can cancel their own future bookings; the slot becomes AVAILABLE again.
 *  - Leads can cancel a booking on their slot, or cancel the whole slot.
 */
@Service
public class AppointmentService {

    private static final Duration MIN_LENGTH = Duration.ofMinutes(5);
    private static final Duration MAX_LENGTH = Duration.ofHours(4);

    private final AppointmentSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public AppointmentService(AppointmentSlotRepository slotRepository,
                              AppointmentRepository appointmentRepository,
                              UserRepository userRepository,
                              JdbcTemplate jdbcTemplate) {
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // =====================  Lead: slots  =====================

    @Transactional
    public SlotResponse createSlot(AuthUser lead, SlotRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
        Duration length = Duration.between(req.startTime(), req.endTime());
        if (length.compareTo(MIN_LENGTH) < 0 || length.compareTo(MAX_LENGTH) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A slot must be between 5 minutes and 4 hours long");
        }
        if (!LocalDateTime.of(req.date(), req.startTime()).isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot create a slot in the past");
        }
        long overlapping = slotRepository.countOverlapping(
                lead.getId(), req.date(), req.startTime(), req.endTime(), SlotStatus.CANCELLED);
        if (overlapping > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "This slot overlaps with one of your existing slots");
        }
        try {
            AppointmentSlot slot = slotRepository.saveAndFlush(
                    new AppointmentSlot(lead.getId(), req.date(), req.startTime(), req.endTime()));
            return toSlotResponse(slot, null, names(Set.of(lead.getId())), true);
        } catch (DataIntegrityViolationException e) {
            // The database exclusion constraint caught an overlap (e.g. two requests at once).
            throw new ApiException(HttpStatus.CONFLICT, "This slot overlaps with one of your existing slots");
        }
    }

    /** The Lead's own slots. upcomingOnly = hide slots that have already started. */
    @Transactional(readOnly = true)
    public List<SlotResponse> leadSlots(AuthUser lead, boolean upcomingOnly) {
        LocalDateTime now = LocalDateTime.now();
        List<AppointmentSlot> slots = slotRepository.findByLeadIdOrderBySlotDateAscStartTimeAsc(lead.getId())
                .stream()
                .filter(s -> !upcomingOnly || s.isInFuture(now))
                .toList();

        // The active booking (if any) of each slot, keyed by slot id.
        Map<Long, Appointment> activeBySlot = new HashMap<>();
        if (!slots.isEmpty()) {
            List<Long> slotIds = slots.stream().map(AppointmentSlot::getId).toList();
            for (Appointment a : appointmentRepository.findBySlotIdInAndStatus(slotIds, AppointmentStatus.BOOKED)) {
                activeBySlot.put(a.getSlot().getId(), a);
            }
        }

        Set<Long> userIds = new HashSet<>();
        userIds.add(lead.getId());
        activeBySlot.values().forEach(a -> userIds.add(a.getStudentId()));
        Map<Long, AppUser> users = names(userIds);

        return slots.stream()
                .map(s -> toSlotResponse(s, activeBySlot.get(s.getId()), users, true))
                .toList();
    }

    /** Cancel a whole slot. If it was booked, the booking is cancelled too. */
    @Transactional
    public SlotResponse cancelSlot(AuthUser lead, Long slotId) {
        AppointmentSlot slot = slotRepository.findById(slotId)
                .filter(s -> s.getLeadId().equals(lead.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Slot not found"));
        if (slot.getStatus() == SlotStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "This slot is already cancelled");
        }
        if (!slot.isInFuture(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Past slots cannot be cancelled");
        }
        appointmentRepository.findFirstBySlotIdAndStatus(slotId, AppointmentStatus.BOOKED)
                .ifPresent(a -> a.cancel(Role.LEAD));
        slot.setStatus(SlotStatus.CANCELLED);
        slotRepository.saveAndFlush(slot);   // fails with 409 if the slot changed meanwhile (@Version)
        return toSlotResponse(slot, null, names(Set.of(lead.getId())), true);
    }

    /** Booking overview for the Lead: all bookings (active and cancelled) on their slots. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> leadBookings(AuthUser lead) {
        return toAppointmentResponses(appointmentRepository.findForLead(lead.getId()));
    }

    // =====================  Student  =====================

    @Transactional(readOnly = true)
    public List<SlotResponse> availableSlots() {
        LocalDateTime now = LocalDateTime.now();
        List<AppointmentSlot> slots = slotRepository.findUpcomingByStatus(
                SlotStatus.AVAILABLE, now.toLocalDate(), now.toLocalTime());
        Map<Long, AppUser> leads = names(slots.stream().map(AppointmentSlot::getLeadId).collect(Collectors.toSet()));
        return slots.stream().map(s -> toSlotResponse(s, null, leads, false)).toList();
    }

    @Transactional
    public AppointmentResponse book(AuthUser student, Long slotId) {
        LocalDateTime now = LocalDateTime.now();

        // Atomic "check and book" using plain JDBC: the WHERE clause only matches if the
        // slot is still AVAILABLE and in the future. PostgreSQL locks the row while
        // updating, so when two students try at once, the second UPDATE sees
        // status = 'BOOKED' and changes 0 rows.
        int updated = jdbcTemplate.update("""
                UPDATE appointment_slots
                   SET status = 'BOOKED', version = version + 1
                 WHERE id = ?
                   AND status = 'AVAILABLE'
                   AND (slot_date + start_time) > ?
                """, slotId, now);

        if (updated == 0) {
            throw explainWhyNotBookable(slotId, now);
        }

        AppointmentSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Slot not found"));
        try {
            Appointment appointment = appointmentRepository.saveAndFlush(new Appointment(slot, student.getId()));
            return toAppointmentResponses(List.of(appointment)).get(0);
        } catch (DataIntegrityViolationException e) {
            // Unique index "one active booking per slot" – should never happen after the UPDATE above.
            throw new ApiException(HttpStatus.CONFLICT, "This slot has just been booked by someone else");
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> myAppointments(AuthUser student) {
        return toAppointmentResponses(appointmentRepository.findForStudent(student.getId()));
    }

    // =====================  Cancelling a booking (student or Lead)  =====================

    @Transactional
    public AppointmentResponse cancelBooking(AuthUser me, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appointment not found"));
        AppointmentSlot slot = appointment.getSlot();

        boolean isOwner = me.getRole() == Role.STUDENT && appointment.getStudentId().equals(me.getId());
        boolean isSlotLead = me.getRole() == Role.LEAD && slot.getLeadId().equals(me.getId());
        if (!isOwner && !isSlotLead) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Appointment not found");
        }
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ApiException(HttpStatus.CONFLICT, "This appointment is already cancelled");
        }
        if (!slot.isInFuture(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Past appointments cannot be cancelled");
        }

        appointment.cancel(me.getRole());
        slot.setStatus(SlotStatus.AVAILABLE);   // free the slot for other students
        appointmentRepository.save(appointment);
        slotRepository.saveAndFlush(slot);
        return toAppointmentResponses(List.of(appointment)).get(0);
    }

    // =====================  helpers  =====================

    private ApiException explainWhyNotBookable(Long slotId, LocalDateTime now) {
        AppointmentSlot slot = slotRepository.findById(slotId).orElse(null);
        if (slot == null) {
            return new ApiException(HttpStatus.NOT_FOUND, "Slot not found");
        }
        if (!slot.isInFuture(now)) {
            return new ApiException(HttpStatus.BAD_REQUEST, "Past slots cannot be booked");
        }
        if (slot.getStatus() == SlotStatus.CANCELLED) {
            return new ApiException(HttpStatus.CONFLICT, "This slot has been cancelled");
        }
        return new ApiException(HttpStatus.CONFLICT, "This slot is already booked");
    }

    private Map<Long, AppUser> names(Collection<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));
    }

    private SlotResponse toSlotResponse(AppointmentSlot s, Appointment booking, Map<Long, AppUser> users,
                                        boolean includeBooking) {
        AppUser lead = users.get(s.getLeadId());
        AppUser student = (includeBooking && booking != null) ? users.get(booking.getStudentId()) : null;
        return new SlotResponse(
                s.getId(),
                s.getSlotDate(),
                s.getStartTime(),
                s.getEndTime(),
                s.getStatus(),
                !s.isInFuture(LocalDateTime.now()),
                s.getLeadId(),
                lead == null ? null : lead.getFullName(),
                includeBooking && booking != null ? booking.getId() : null,
                student == null ? null : student.getId(),
                student == null ? null : student.getFullName(),
                student == null ? null : student.getEmail());
    }

    private List<AppointmentResponse> toAppointmentResponses(List<Appointment> appointments) {
        Set<Long> userIds = new HashSet<>();
        appointments.forEach(a -> {
            userIds.add(a.getStudentId());
            userIds.add(a.getSlot().getLeadId());
        });
        Map<Long, AppUser> users = names(userIds);
        LocalDateTime now = LocalDateTime.now();

        return appointments.stream().map(a -> {
            AppointmentSlot s = a.getSlot();
            AppUser lead = users.get(s.getLeadId());
            AppUser student = users.get(a.getStudentId());
            boolean upcoming = s.isInFuture(now);
            boolean cancellable = upcoming && a.getStatus() == AppointmentStatus.BOOKED;
            return new AppointmentResponse(
                    a.getId(),
                    a.getStatus(),
                    a.getBookedAt(),
                    a.getCancelledAt(),
                    a.getCancelledBy(),
                    s.getId(),
                    s.getSlotDate(),
                    s.getStartTime(),
                    s.getEndTime(),
                    upcoming,
                    cancellable,
                    s.getLeadId(),
                    lead == null ? null : lead.getFullName(),
                    a.getStudentId(),
                    student == null ? null : student.getFullName(),
                    // a student's own email is fine to show them; Leads need it to contact the student
                    student == null ? null : student.getEmail());
        }).toList();
    }
}
