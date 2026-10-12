-- V6: Lead review / verification workflow + extra fields from the OGE intake form.

-- ---------- extra profile fields (exchange student intake) ----------
ALTER TABLE student_profiles
    ADD COLUMN gender          VARCHAR(20),
    ADD COLUMN home_university VARCHAR(150),
    ADD COLUMN passport_number VARCHAR(30),
    ADD COLUMN passport_expiry DATE,
    ADD COLUMN visa_expiry     DATE,
    ADD COLUMN arrival_date    DATE,
    ADD COLUMN departure_date  DATE,
    ADD CONSTRAINT ck_profiles_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'));

-- ---------- review status of the profile and of the hostel details ----------
-- PENDING = waiting for the Lead, VERIFIED = checked, NEEDS_CORRECTION = student must fix (see comment)
ALTER TABLE student_profiles
    ADD COLUMN profile_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    ADD COLUMN profile_comment      VARCHAR(500),
    ADD COLUMN profile_reviewed_at  TIMESTAMPTZ,
    ADD COLUMN profile_reviewed_by  BIGINT REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN hostel_review_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    ADD COLUMN hostel_comment       VARCHAR(500),
    ADD COLUMN hostel_reviewed_at   TIMESTAMPTZ,
    ADD COLUMN hostel_reviewed_by   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    ADD CONSTRAINT ck_profiles_profile_status CHECK (profile_status IN ('PENDING', 'VERIFIED', 'NEEDS_CORRECTION')),
    ADD CONSTRAINT ck_profiles_hostel_review  CHECK (hostel_review_status IN ('PENDING', 'VERIFIED', 'NEEDS_CORRECTION'));

-- ---------- review status of each uploaded document ----------
-- UPLOADED = waiting for the Lead's review
ALTER TABLE documents DROP CONSTRAINT ck_documents_status;
ALTER TABLE documents
    ADD COLUMN review_comment VARCHAR(500),
    ADD COLUMN reviewed_at    TIMESTAMPTZ,
    ADD COLUMN reviewed_by    BIGINT REFERENCES users (id) ON DELETE SET NULL,
    ADD CONSTRAINT ck_documents_status CHECK (status IN ('UPLOADED', 'VERIFIED', 'NEEDS_CORRECTION'));
