package com.example.student_management_system.student.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of the Lead's review endpoints, e.g.
 *   {"decision":"VERIFIED"}
 *   {"decision":"NEEDS_CORRECTION","comment":"The passport scan is cut off, please upload the full page."}
 * A comment is required when asking for a correction (checked in ReviewService).
 */
public record ReviewRequest(
        @NotNull(message = "Decision is required")
        @Pattern(regexp = "VERIFIED|NEEDS_CORRECTION", message = "Decision must be VERIFIED or NEEDS_CORRECTION")
        String decision,

        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {}
