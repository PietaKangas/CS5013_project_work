-- V2: student profile data, kept separate from the login data in "users".
-- One row per student account (user_id is UNIQUE).
CREATE TABLE student_profiles (
    id                BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    -- basic / contact information
    phone             VARCHAR(30),
    date_of_birth     DATE,
    nationality       VARCHAR(60),
    address           VARCHAR(255),
    emergency_contact VARCHAR(255),

    -- academic information
    student_number    VARCHAR(30),
    programme         VARCHAR(120),
    department        VARCHAR(120),
    academic_year     VARCHAR(40),
    student_category  VARCHAR(20),   -- decides which documents are required

    -- hostel information
    hostel_name       VARCHAR(120),
    room_number       VARCHAR(20),
    hostel_status     VARCHAR(20)  NOT NULL DEFAULT 'NOT_APPLIED',

    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_profiles_user           UNIQUE (user_id),
    CONSTRAINT uk_profiles_student_number UNIQUE (student_number),
    CONSTRAINT ck_profiles_category       CHECK (student_category IN ('DEGREE', 'EXCHANGE')),
    CONSTRAINT ck_profiles_hostel_status  CHECK (hostel_status IN ('NOT_APPLIED', 'APPLIED', 'ALLOCATED'))
);
