package com.example.student_management_system.authentication.dto;
 
// This class represents a response after transferring the lead role from one user to another.
public class LeadTransferResponse {

    private String currentLeadEmail;
    private String newLeadEmail;
    private String message;

    public LeadTransferResponse() {}

    public LeadTransferResponse(String currentLeadEmail, String newLeadEmail, String message) {
        this.currentLeadEmail = currentLeadEmail;
        this.newLeadEmail = newLeadEmail;
        this.message = message;
    }

    public String getCurrentLeadEmail() {
        return currentLeadEmail;
    }

    public void setCurrentLeadEmail(String currentLeadEmail) {
        this.currentLeadEmail = currentLeadEmail;
    }

    public String getNewLeadEmail() {
        return newLeadEmail;
    }

    public void setNewLeadEmail(String newLeadEmail) {
        this.newLeadEmail = newLeadEmail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
