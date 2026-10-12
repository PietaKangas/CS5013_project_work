-- V4: simple appointments.
-- A Lead creates time slots; a student books a free slot. Bookings are kept as
-- separate rows so cancelled bookings stay in the history.

-- Needed for the "no overlapping slots" rule below (ships with PostgreSQL).
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE appointment_slots (
    id         BIGSERIAL   PRIMARY KEY,
    lead_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    slot_date  DATE        NOT NULL,
    start_time TIME        NOT NULL,
    end_time   TIME        NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version    BIGINT      NOT NULL DEFAULT 0,      -- optimistic locking (see AppointmentSlot.java)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_slots_times  CHECK (end_time > start_time),
    CONSTRAINT ck_slots_status CHECK (status IN ('AVAILABLE', 'BOOKED', 'CANCELLED')),
    -- The database itself refuses two non-cancelled slots of the same Lead that overlap in time.
    CONSTRAINT ex_slots_no_overlap EXCLUDE USING gist (
        lead_id WITH =,
        tsrange(slot_date + start_time, slot_date + end_time) WITH &&
    ) WHERE (status <> 'CANCELLED')
);

CREATE INDEX idx_slots_date ON appointment_slots (slot_date, start_time);

CREATE TABLE appointments (
    id           BIGSERIAL   PRIMARY KEY,
    slot_id      BIGINT      NOT NULL REFERENCES appointment_slots (id) ON DELETE CASCADE,
    student_id   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    booked_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMPTZ,
    cancelled_by VARCHAR(20),
    CONSTRAINT ck_appointments_status       CHECK (status IN ('BOOKED', 'CANCELLED')),
    CONSTRAINT ck_appointments_cancelled_by CHECK (cancelled_by IN ('STUDENT', 'LEAD'))
);

-- Safety net against double booking: at most ONE active booking per slot.
CREATE UNIQUE INDEX uk_appointments_one_active_per_slot ON appointments (slot_id) WHERE status = 'BOOKED';
CREATE INDEX idx_appointments_student ON appointments (student_id);
