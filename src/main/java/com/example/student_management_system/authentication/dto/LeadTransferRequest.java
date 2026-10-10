package com.example.student_management_system.authentication.dto;

public class LeadTransferRequest {

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
