package com.example.student_management_system.document;

import com.example.student_management_system.student.StudentCategory;

import java.util.List;
import java.util.Map;

/**
 * Document status for one student, calculated from the required types and the student's
 * actual uploads and their review status (never a manually typed percentage).
 */
public record DocumentCompletion(
        List<DocumentType> requiredTypes,
        int requiredCount,
        int uploadedCount,
        int verifiedCount,
        List<DocumentType> missingTypes,
        List<DocumentType> correctionTypes,
        List<DocumentType> awaitingReviewTypes,
        boolean complete,
        boolean allVerified,
        int percent
) {
    /**
     * @param activeTypes    all active document types
     * @param category       the student's category (may be null)
     * @param uploadedStatus document type id -> status of this student's upload for that type
     */
    public static DocumentCompletion of(List<DocumentType> activeTypes,
                                        StudentCategory category,
                                        Map<Long, DocumentStatus> uploadedStatus) {
        List<DocumentType> required = activeTypes.stream().filter(t -> t.isRequiredFor(category)).toList();
        List<DocumentType> missing = required.stream()
                .filter(t -> !uploadedStatus.containsKey(t.getId())).toList();
        List<DocumentType> corrections = required.stream()
                .filter(t -> uploadedStatus.get(t.getId()) == DocumentStatus.NEEDS_CORRECTION).toList();
        List<DocumentType> awaiting = required.stream()
                .filter(t -> uploadedStatus.get(t.getId()) == DocumentStatus.UPLOADED).toList();
        int verified = (int) required.stream()
                .filter(t -> uploadedStatus.get(t.getId()) == DocumentStatus.VERIFIED).count();

        int uploaded = required.size() - missing.size();
        int percent = required.isEmpty() ? 100 : Math.round(uploaded * 100f / required.size());
        return new DocumentCompletion(required, required.size(), uploaded, verified, missing, corrections, awaiting,
                missing.isEmpty(), verified == required.size(), percent);
    }
}
