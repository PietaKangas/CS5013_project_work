-- V3: configurable document types + uploaded documents.

-- Which documents students must upload. student_category NULL = required for every
-- student; 'DEGREE' / 'EXCHANGE' = only for that kind of student.
-- Leads can add types or switch them off (active = false) via /api/admin/document-types.
CREATE TABLE document_types (
    id               BIGSERIAL    PRIMARY KEY,
    code             VARCHAR(50)  NOT NULL,
    name             VARCHAR(100) NOT NULL,
    description      VARCHAR(255),
    student_category VARCHAR(20),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order       INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uk_document_types_code     UNIQUE (code),
    CONSTRAINT ck_document_types_category CHECK (student_category IN ('DEGREE', 'EXCHANGE'))
);

INSERT INTO document_types (code, name, description, student_category, sort_order) VALUES
    ('PASSPORT',           'Passport',                         'Photo/data page of a valid passport',              NULL,       10),
    ('VISA',               'Visa / residence permit',          'Student visa or residence permit',                 NULL,       20),
    ('ADMISSION_LETTER',   'Admission letter',                 'Official letter of admission from the university', NULL,       30),
    ('HEALTH_INSURANCE',   'Health insurance',                 'Proof of health insurance for the study period',   NULL,       40),
    ('PHOTO',              'Passport-size photo',              'Recent photo with plain background',               NULL,       50),
    ('DEGREE_CERTIFICATE', 'Previous degree certificate',      'Certificate and transcripts of previous degree',   'DEGREE',   60),
    ('LEARNING_AGREEMENT', 'Learning agreement',               'Signed exchange learning agreement',               'EXCHANGE', 70);

-- One row per uploaded file. A student has at most one file per document type
-- (uploading again replaces it). The file itself is on disk under a random name
-- (stored_filename); that name/path is never sent to the browser.
CREATE TABLE documents (
    id                BIGSERIAL    PRIMARY KEY,
    student_id        BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    document_type_id  BIGINT       NOT NULL REFERENCES document_types (id),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(100) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED',
    uploaded_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_documents_student_type UNIQUE (student_id, document_type_id),
    CONSTRAINT uk_documents_stored_name  UNIQUE (stored_filename),
    CONSTRAINT ck_documents_status       CHECK (status IN ('UPLOADED'))
);

CREATE INDEX idx_documents_student ON documents (student_id);
