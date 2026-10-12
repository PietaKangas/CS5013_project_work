package com.example.student_management_system.document;

import com.example.student_management_system.student.StudentCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A kind of document students must upload, e.g. "Passport". */
@Entity
@Table(name = "document_types")
public class DocumentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    /** null = required for every student. */
    @Enumerated(EnumType.STRING)
    @Column(name = "student_category")
    private StudentCategory studentCategory;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Optional link to a form the student must download/fill in (e.g. the iKollege BIODATA form). */
    @Column(name = "form_url")
    private String formUrl;

    protected DocumentType() {
        // required by JPA
    }

    public DocumentType(String code, String name, String description, StudentCategory studentCategory, int sortOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.studentCategory = studentCategory;
        this.sortOrder = sortOrder;
    }

    /** Is this type required for a student of the given category (null = category not chosen yet)? */
    public boolean isRequiredFor(StudentCategory category) {
        return active && (studentCategory == null || studentCategory == category);
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public StudentCategory getStudentCategory() { return studentCategory; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
    public String getFormUrl() { return formUrl; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStudentCategory(StudentCategory studentCategory) { this.studentCategory = studentCategory; }
    public void setActive(boolean active) { this.active = active; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setFormUrl(String formUrl) { this.formUrl = formUrl; }
}
