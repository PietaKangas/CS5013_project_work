package com.example.student_management_system.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** A time slot offered by a Lead. */
@Entity
@Table(name = "appointment_slots")
public class AppointmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id", nullable = false, updatable = false)
    private Long leadId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status = SlotStatus.AVAILABLE;

    /**
     * Optimistic locking: Hibernate adds "AND version = ?" to every UPDATE.
     * If two people change the same slot at once, the second update fails instead of
     * silently overwriting the first (GlobalExceptionHandler turns that into 409).
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppointmentSlot() {
        // required by JPA
    }

    public AppointmentSlot(Long leadId, LocalDate slotDate, LocalTime startTime, LocalTime endTime) {
        this.leadId = leadId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public LocalDateTime startsAt() {
        return LocalDateTime.of(slotDate, startTime);
    }

    public boolean isInFuture(LocalDateTime now) {
        return startsAt().isAfter(now);
    }

    public Long getId() { return id; }
    public Long getLeadId() { return leadId; }
    public LocalDate getSlotDate() { return slotDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public SlotStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(SlotStatus status) { this.status = status; }
}
