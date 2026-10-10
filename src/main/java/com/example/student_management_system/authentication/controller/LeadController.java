package com.example.student_management_system.authentication.controller;

import com.example.student_management_system.authentication.dto.LeadTransferRequest;
import com.example.student_management_system.authentication.dto.LeadTransferResponse;
import com.example.student_management_system.authentication.service.LeadTransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/lead")
public class LeadController {

    private final LeadTransferService adminLeadService;

    public LeadController(LeadTransferService adminLeadService) {
        this.adminLeadService = adminLeadService;
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('LEAD')")
    public ResponseEntity<LeadTransferResponse> transferLead(@RequestBody LeadTransferRequest request) {
        if (request == null || request.getNewLeadUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newLeadUserId is required");
        }

        LeadTransferResponse response = adminLeadService.transferLead(request.getNewLeadUserId());
        return ResponseEntity.ok(response);
    }
}
