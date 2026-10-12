package com.example.student_management_system.auth;

import com.example.student_management_system.auth.dto.SignupRequest;
import com.example.student_management_system.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/admin/leads -> create another International Lead account.
 * SecurityConfig restricts /api/admin/** to users with the LEAD role, so only an
 * existing Lead can create new Leads. Nobody can make themselves a Lead via signup.
 */
@RestController
@RequestMapping("/api/admin/leads")
public class LeadAccountController {

    private final AuthService authService;

    public LeadAccountController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createLead(@Valid @RequestBody SignupRequest request) {
        return authService.createLead(request);
    }
}
