package com.agenthub.auth.dto;

public class RegisterRequest {

    private Long customerId;
    private String username;
    private String email;
    private String password;

    public RegisterRequest() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public RegisterRequest setCustomerId(Long customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public RegisterRequest setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public RegisterRequest setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public RegisterRequest setPassword(String password) {
        this.password = password;
        return this;
    }
}
