package com.example.student_management_system.appointment;

import com.example.student_management_system.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/** A student's booking of a slot. Cancelled bookings are kept for the history. */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "slot_id", nullable = false, updatable = false)
    private AppointmentSlot slot;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Column(name = "booked_at", nullable = false, updatable = false)
    private Instant bookedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by")
    private Role cancelledBy;

    protected Appointment() {
        // required by JPA
    }

    public Appointment(AppointmentSlot slot, Long studentId) {
        this.slot = slot;
        this.studentId = studentId;
    }

    @PrePersist
    void onCreate() {
        this.bookedAt = Instant.now();
    }

    public void cancel(Role by) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = by;
    }

    public Long getId() { return id; }
    public AppointmentSlot getSlot() { return slot; }
    public Long getStudentId() { return studentId; }
    public AppointmentStatus getStatus() { return status; }
    public Instant getBookedAt() { return bookedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Role getCancelledBy() { return cancelledBy; }
}
