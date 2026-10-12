package com.example.student_management_system.student;

import com.example.student_management_system.common.ApiException;
import com.example.student_management_system.common.PageResponse;
import com.example.student_management_system.security.AuthUser;
import com.example.student_management_system.student.dto.HostelUpdateRequest;
import com.example.student_management_system.student.dto.ProfileResponse;
import com.example.student_management_system.student.dto.ProfileUpdateRequest;
import com.example.student_management_system.student.dto.StudentStats;
import com.example.student_management_system.student.dto.StudentSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {id} is always the student's account (database) id.
 *
 * Lead only:
 *   GET /api/students                         list with search / filter / sort / paging
 *   GET /api/students/stats                   dashboard numbers
 *   GET /api/students/by-student-number/{n}   find by university student ID
 * Student (own data) or Lead (any student):
 *   GET /api/students/{id}                    summary row incl. completion
 *   GET /api/students/{id}/profile            full profile
 * Student, own data only:
 *   GET /api/students/me/profile              shortcut for the logged-in student
 *   PUT /api/students/{id}/profile            update basic + academic info
 * Student (own) or Lead:
 *   PUT /api/students/{id}/hostel             update hostel info (a Lead's update counts as verified)
 * Review endpoints are in ReviewController.
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final ProfileService profileService;
    private final StudentDirectoryService directoryService;
    private final StudentAccess access;

    public StudentController(ProfileService profileService,
                             StudentDirectoryService directoryService,
                             StudentAccess access) {
        this.profileService = profileService;
        this.directoryService = directoryService;
        this.access = access;
    }

    @GetMapping
    @PreAuthorize("hasRole('LEAD')")
    public PageResponse<StudentSummary> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean profileComplete,
            @RequestParam(required = false) Boolean documentsComplete,
            @RequestParam(required = false) HostelStatus hostelStatus,
            @RequestParam(required = false) Long missingDocumentTypeId,
            @RequestParam(required = false) String reviewState,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return directoryService.search(new StudentDirectoryService.Query(
                search, profileComplete, documentsComplete, hostelStatus, missingDocumentTypeId,
                reviewState, sort, direction, page, size));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('LEAD')")
    public StudentStats stats() {
        return directoryService.stats();
    }

    @GetMapping("/by-student-number/{studentNumber}")
    @PreAuthorize("hasRole('LEAD')")
    public ProfileResponse byStudentNumber(@PathVariable String studentNumber) {
        return profileService.getByStudentNumber(studentNumber);
    }

    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ProfileResponse myProfile(@AuthenticationPrincipal AuthUser me) {
        return profileService.getProfile(me, me.getId());
    }

    @GetMapping("/{id}")
    public StudentSummary summary(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        access.requireViewable(me, id);
        StudentSummary summary = directoryService.summaryOf(id);
        if (summary == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Student not found");
        }
        return summary;
    }

    @GetMapping("/{id}/profile")
    public ProfileResponse profile(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return profileService.getProfile(me, id);
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ProfileResponse updateProfile(@AuthenticationPrincipal AuthUser me,
                                         @PathVariable Long id,
                                         @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(me, id, request);
    }

    // The student, or a Lead correcting the details (StudentAccess.requireSelfOrLead checks this)
    @PutMapping("/{id}/hostel")
    public ProfileResponse updateHostel(@AuthenticationPrincipal AuthUser me,
                                        @PathVariable Long id,
                                        @Valid @RequestBody HostelUpdateRequest request) {
        return profileService.updateHostel(me, id, request);
    }
}
