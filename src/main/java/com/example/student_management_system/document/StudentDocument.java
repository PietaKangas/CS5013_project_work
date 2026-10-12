package com.example.student_management_system.document;

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
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An uploaded file's metadata ("documents" table). Named StudentDocument to avoid
 * confusion with other classes called Document.
 */
@Entity
@Table(name = "documents")
public class StudentDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    // EAGER: the type is tiny and we almost always need its name.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "document_type_id", nullable = false, updatable = false)
    private DocumentType documentType;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    // Random name of the file on disk. Never returned by the API.
    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    // ---- Lead review ----
    @Column(name = "review_comment")
    private String reviewComment;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    protected StudentDocument() {
        // required by JPA
    }

    public StudentDocument(Long studentId, DocumentType documentType) {
        this.studentId = studentId;
        this.documentType = documentType;
    }

    /** Point this row at a newly stored file (first upload or replacement). */
    public void attachFile(String originalFilename, String storedFilename, String contentType, long sizeBytes) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = DocumentStatus.UPLOADED;   // a new file always needs a new review
        this.uploadedAt = Instant.now();
        this.reviewComment = null;
        this.reviewedAt = null;
        this.reviewedBy = null;
    }

    /** Called when the Lead verifies the document or asks for a correction. */
    public void review(DocumentStatus newStatus, String comment, Long leadId) {
        this.status = newStatus;
        this.reviewComment = comment;
        this.reviewedAt = Instant.now();
        this.reviewedBy = leadId;
    }

    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public DocumentType getDocumentType() { return documentType; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public DocumentStatus getStatus() { return status; }
    public Instant getUploadedAt() { return uploadedAt; }
    public String getReviewComment() { return reviewComment; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Long getReviewedBy() { return reviewedBy; }
}
