package com.example.student_management_system.student;

import com.example.student_management_system.common.ApiException;
import com.example.student_management_system.document.DocumentStatus;
import com.example.student_management_system.document.StudentDocument;
import com.example.student_management_system.document.StudentDocumentRepository;
import com.example.student_management_system.document.dto.DocumentResponse;
import com.example.student_management_system.security.AuthUser;
import com.example.student_management_system.student.dto.ProfileResponse;
import com.example.student_management_system.student.dto.ReviewRequest;
import com.example.student_management_system.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The International Lead's review: mark a student's profile, hostel details or a single
 * document as VERIFIED, or send it back with NEEDS_CORRECTION and a comment the student sees.
 * Only Leads reach these methods (checked with @PreAuthorize in ReviewController).
 */
@Service
public class ReviewService {

    private final StudentProfileRepository profileRepository;
    private final StudentDocumentRepository documentRepository;
    private final StudentAccess access;

    public ReviewService(StudentProfileRepository profileRepository,
                         StudentDocumentRepository documentRepository,
                         StudentAccess access) {
        this.profileRepository = profileRepository;
        this.documentRepository = documentRepository;
        this.access = access;
    }

    @Transactional
    public ProfileResponse reviewProfile(AuthUser lead, Long studentId, ReviewRequest req) {
        AppUser student = access.requireStudent(studentId);
        StudentProfile p = profileRepository.findByUserId(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This student hasn't filled in a profile yet"));
        ReviewStatus decision = ReviewStatus.valueOf(req.decision());
        String comment = checkComment(decision, req.comment());

        if (decision == ReviewStatus.VERIFIED && !ProfileCompletion.of(student.getFullName(), p).complete()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "An incomplete profile can't be verified. Ask the student to complete it instead.");
        }
        p.reviewProfile(decision, comment, lead.getId());
        return ProfileResponse.from(student, profileRepository.saveAndFlush(p));
    }

    @Transactional
    public ProfileResponse reviewHostel(AuthUser lead, Long studentId, ReviewRequest req) {
        AppUser student = access.requireStudent(studentId);
        StudentProfile p = profileRepository.findByUserId(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This student hasn't entered hostel details yet"));
        ReviewStatus decision = ReviewStatus.valueOf(req.decision());
        p.reviewHostel(decision, checkComment(decision, req.comment()), lead.getId());
        return ProfileResponse.from(student, profileRepository.saveAndFlush(p));
    }

    @Transactional
    public DocumentResponse reviewDocument(AuthUser lead, Long documentId, ReviewRequest req) {
        StudentDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found"));
        DocumentStatus decision = DocumentStatus.valueOf(req.decision());
        String comment = checkComment(
                decision == DocumentStatus.VERIFIED ? ReviewStatus.VERIFIED : ReviewStatus.NEEDS_CORRECTION,
                req.comment());
        doc.review(decision, comment, lead.getId());
        return DocumentResponse.from(documentRepository.saveAndFlush(doc));
    }

    /** A correction request must say what to fix; a verification may have an optional note. */
    private static String checkComment(ReviewStatus decision, String comment) {
        String c = comment == null ? null : comment.trim();
        if (c != null && c.isEmpty()) c = null;
        if (decision == ReviewStatus.NEEDS_CORRECTION && c == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please tell the student what needs to be corrected");
        }
        return c;
    }
}
