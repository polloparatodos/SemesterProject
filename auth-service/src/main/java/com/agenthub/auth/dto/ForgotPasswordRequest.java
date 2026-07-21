package com.agenthub.auth.dto;

public class ForgotPasswordRequest {

    private String email;

    public ForgotPasswordRequest() {
    }

    public String getEmail() {
        return email;
    }

    public ForgotPasswordRequest setEmail(String email) {
        this.email = email;
        return this;
    }
}
