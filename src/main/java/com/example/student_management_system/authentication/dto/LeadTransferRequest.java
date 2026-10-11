package com.example.student_management_system.authentication.dto;

import jakarta.validation.constraints.NotNull;

// This class represents a request to transfer the lead role from the current lead user to a new lead user.
public class LeadTransferRequest {

    @NotNull(message = "newLeadUserId is required")
    private Long newLeadUserId;

    public LeadTransferRequest() {}

    public LeadTransferRequest(Long newLeadUserId) {
        this.newLeadUserId = newLeadUserId;
    }

    public Long getNewLeadUserId() {
        return newLeadUserId;
    }

    public void setNewLeadUserId(Long newLeadUserId) {
        this.newLeadUserId = newLeadUserId;
    }
}
