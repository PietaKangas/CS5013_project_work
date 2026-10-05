package com.example.student_management_system.authentication.dto;

public class SignupRequest {

    private String email;
    private String password;

    public SignupRequest() {}

    // removed string role
    public SignupRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


}
