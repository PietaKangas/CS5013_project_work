package com.example.student_management_system.student;

import com.example.student_management_system.common.PageResponse;
import com.example.student_management_system.document.DocumentCompletion;
import com.example.student_management_system.document.DocumentService;
import com.example.student_management_system.document.DocumentStatus;
import com.example.student_management_system.document.DocumentType;
import com.example.student_management_system.document.DocumentTypeRepository;
import com.example.student_management_system.student.dto.StudentStats;
import com.example.student_management_system.student.dto.StudentSummary;
import com.example.student_management_system.user.AppUser;
import com.example.student_management_system.user.Role;
import com.example.student_management_system.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds the Lead's student overview: one summary row per student with profile, hostel
 * and document status, then applies search, filters, sorting and paging.
 *
 * For a small project we load all students (a few hundred at most) and do the
 * filtering in Java. This keeps the code easy to follow; with thousands of
 * students you would move the filtering into SQL queries instead.
 */
@Service
public class StudentDirectoryService {

    public static final String NEEDS_CORRECTION = "NEEDS_CORRECTION";
    public static final String AWAITING_REVIEW = "AWAITING_REVIEW";
    public static final String INCOMPLETE = "INCOMPLETE";
    public static final String VERIFIED = "VERIFIED";

    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final DocumentTypeRepository typeRepository;
    private final DocumentService documentService;

    public StudentDirectoryService(UserRepository userRepository,
                                   StudentProfileRepository profileRepository,
                                   DocumentTypeRepository typeRepository,
                                   DocumentService documentService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.typeRepository = typeRepository;
        this.documentService = documentService;
    }

    /** Search/filter options from the query string. null = "don't filter on this". */
    public record Query(
            String search,
            Boolean profileComplete,
            Boolean documentsComplete,
            HostelStatus hostelStatus,
            Long missingDocumentTypeId,
            String reviewState,  // NEEDS_CORRECTION | AWAITING_REVIEW | INCOMPLETE | VERIFIED
            String sort,         // name | studentNumber | profile | documents | review
            String direction,    // asc | desc
            int page,
            int size
    ) {}

    @Transactional(readOnly = true)
    public PageResponse<StudentSummary> search(Query q) {
        Stream<StudentSummary> rows = allSummaries().stream();

        if (q.search() != null && !q.search().isBlank()) {
            String needle = q.search().trim().toLowerCase(Locale.ROOT);
            rows = rows.filter(s -> contains(s.fullName(), needle)
                    || contains(s.email(), needle)
                    || contains(s.studentNumber(), needle)
                    || contains(s.homeUniversity(), needle)
                    || contains(s.nationality(), needle)
                    || contains(s.roomNumber(), needle));
        }
        if (q.profileComplete() != null) {
            rows = rows.filter(s -> s.profileComplete() == q.profileComplete());
        }
        if (q.documentsComplete() != null) {
            rows = rows.filter(s -> s.documentsComplete() == q.documentsComplete());
        }
        if (q.hostelStatus() != null) {
            rows = rows.filter(s -> s.hostelStatus() == q.hostelStatus());
        }
        if (q.missingDocumentTypeId() != null) {
            rows = rows.filter(s -> s.missingDocumentTypeIds().contains(q.missingDocumentTypeId()));
        }
        if (q.reviewState() != null && !q.reviewState().isBlank()) {
            rows = rows.filter(s -> s.reviewState().equalsIgnoreCase(q.reviewState()));
        }

        List<StudentSummary> sorted = rows.sorted(comparator(q.sort(), q.direction())).toList();
        return PageResponse.of(sorted, q.page(), q.size());
    }

    @Transactional(readOnly = true)
    public StudentSummary summaryOf(Long studentId) {
        return allSummaries().stream()
                .filter(s -> s.id().equals(studentId))
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public StudentStats stats() {
        List<StudentSummary> all = allSummaries();
        return new StudentStats(
                all.size(),
                all.stream().filter(StudentSummary::profileComplete).count(),
                all.stream().filter(StudentSummary::documentsComplete).count(),
                all.stream().filter(s -> s.hostelStatus() == HostelStatus.ALLOCATED).count(),
                all.stream().filter(s -> AWAITING_REVIEW.equals(s.reviewState())).count(),
                all.stream().filter(s -> NEEDS_CORRECTION.equals(s.reviewState())).count(),
                all.stream().filter(s -> VERIFIED.equals(s.reviewState())).count());
    }

    // ---------- building the rows ----------

    private List<StudentSummary> allSummaries() {
        List<AppUser> students = userRepository.findByRole(Role.STUDENT);
        Map<Long, StudentProfile> profiles = profileRepository.findAll().stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));
        List<DocumentType> activeTypes = typeRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
        Map<Long, Map<Long, DocumentStatus>> documents = documentService.documentStatusesByStudent();

        return students.stream()
                .map(u -> toSummary(u, profiles.get(u.getId()), activeTypes,
                        documents.getOrDefault(u.getId(), Map.of())))
                .toList();
    }

    private StudentSummary toSummary(AppUser u, StudentProfile p, List<DocumentType> activeTypes,
                                     Map<Long, DocumentStatus> uploaded) {
        ProfileCompletion pc = ProfileCompletion.of(u.getFullName(), p);
        StudentCategory category = p == null ? null : p.getStudentCategory();
        DocumentCompletion dc = DocumentCompletion.of(activeTypes, category, uploaded);

        ReviewStatus profileStatus = p == null ? ReviewStatus.PENDING : p.getProfileStatus();
        ReviewStatus hostelReview = p == null ? ReviewStatus.PENDING : p.getHostelReviewStatus();
        HostelStatus hostelStatus = p == null ? HostelStatus.NOT_APPLIED : p.getHostelStatus();
        boolean hostelEntered = hostelStatus != HostelStatus.NOT_APPLIED;

        String state;
        if (profileStatus == ReviewStatus.NEEDS_CORRECTION || hostelReview == ReviewStatus.NEEDS_CORRECTION
                || !dc.correctionTypes().isEmpty()) {
            state = NEEDS_CORRECTION;
        } else if ((pc.complete() && profileStatus == ReviewStatus.PENDING)
                || (hostelEntered && hostelReview == ReviewStatus.PENDING)
                || !dc.awaitingReviewTypes().isEmpty()) {
            state = AWAITING_REVIEW;
        } else if (pc.complete() && profileStatus == ReviewStatus.VERIFIED && dc.allVerified()
                && hostelStatus == HostelStatus.ALLOCATED && hostelReview == ReviewStatus.VERIFIED) {
            state = VERIFIED;
        } else {
            state = INCOMPLETE;
        }

        return new StudentSummary(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                p == null ? null : p.getStudentNumber(),
                p == null ? null : p.getNationality(),
                p == null ? null : p.getHomeUniversity(),
                p == null ? null : p.getProgramme(),
                p == null ? null : p.getDepartment(),
                category,
                hostelStatus,
                p == null ? null : p.getHostelName(),
                p == null ? null : p.getRoomNumber(),
                pc.complete(),
                pc.percent(),
                profileStatus,
                hostelReview,
                dc.uploadedCount(),
                dc.requiredCount(),
                dc.verifiedCount(),
                dc.complete(),
                dc.percent(),
                dc.missingTypes().stream().map(DocumentType::getName).toList(),
                dc.missingTypes().stream().map(DocumentType::getId).toList(),
                dc.correctionTypes().stream().map(DocumentType::getName).toList(),
                dc.awaitingReviewTypes().size(),
                state);
    }

    /** Lower number = needs the Lead's attention first. */
    private static int reviewPriority(String state) {
        return switch (state) {
            case AWAITING_REVIEW -> 0;
            case NEEDS_CORRECTION -> 1;
            case INCOMPLETE -> 2;
            default -> 3;
        };
    }

    private static Comparator<StudentSummary> comparator(String sort, String direction) {
        Comparator<StudentSummary> byName =
                Comparator.comparing(s -> s.fullName().toLowerCase(Locale.ROOT));
        Comparator<StudentSummary> c = switch (sort == null ? "name" : sort) {
            case "studentNumber" -> Comparator.comparing(StudentSummary::studentNumber,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "profile" -> Comparator.comparingInt(StudentSummary::profileCompletionPercent);
            case "documents" -> Comparator.comparingInt(StudentSummary::documentCompletionPercent);
            case "review" -> Comparator.comparingInt(s -> reviewPriority(s.reviewState()));
            case "email" -> Comparator.comparing(s -> s.email().toLowerCase(Locale.ROOT));
            default -> byName;
        };
        if ("desc".equalsIgnoreCase(direction)) {
            c = c.reversed();
        }
        return c.thenComparing(byName);   // stable order for ties
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
