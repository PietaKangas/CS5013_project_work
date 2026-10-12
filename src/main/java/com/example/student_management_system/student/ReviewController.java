package com.example.student_management_system.student;

import com.example.student_management_system.document.dto.DocumentResponse;
import com.example.student_management_system.security.AuthUser;
import com.example.student_management_system.student.dto.ProfileResponse;
import com.example.student_management_system.student.dto.ReviewRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lead-only review endpoints. Body: {"decision":"VERIFIED"|"NEEDS_CORRECTION","comment":"..."}
 *   PUT /api/students/{id}/profile/review     review personal + academic details
 *   PUT /api/students/{id}/hostel/review      review hostel + room details
 *   PUT /api/documents/{id}/review            review one uploaded document
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('LEAD')")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PutMapping("/students/{id}/profile/review")
    public ProfileResponse reviewProfile(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                         @Valid @RequestBody ReviewRequest request) {
        return reviewService.reviewProfile(me, id, request);
    }

    @PutMapping("/students/{id}/hostel/review")
    public ProfileResponse reviewHostel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                        @Valid @RequestBody ReviewRequest request) {
        return reviewService.reviewHostel(me, id, request);
    }

    @PutMapping("/documents/{id}/review")
    public DocumentResponse reviewDocument(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                           @Valid @RequestBody ReviewRequest request) {
        return reviewService.reviewDocument(me, id, request);
    }
}
