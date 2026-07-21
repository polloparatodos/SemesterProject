package com.agenthub.auth.dto;

public class UserInfoResponse {

    private Long id;
    private String username;
    private String email;
    private Long customerId;

    public UserInfoResponse() {
    }

    public UserInfoResponse(Long id, String username, String email, Long customerId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.customerId = customerId;
    }

    public Long getId() {
        return id;
    }

    public UserInfoResponse setId(Long id) {
        this.id = id;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public UserInfoResponse setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserInfoResponse setEmail(String email) {
        this.email = email;
        return this;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public UserInfoResponse setCustomerId(Long customerId) {
        this.customerId = customerId;
        return this;
    }
}
