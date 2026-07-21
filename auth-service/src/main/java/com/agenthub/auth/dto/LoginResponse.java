package com.agenthub.auth.dto;

public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private Long expiresIn;
    private String username;
    private Long customerId;

    public LoginResponse() {
    }

    public LoginResponse(String token, Long expiresIn, String username, Long customerId) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.username = username;
        this.customerId = customerId;
    }

    public String getToken() {
        return token;
    }

    public LoginResponse setToken(String token) {
        this.token = token;
        return this;
    }

    public String getType() {
        return type;
    }

    public LoginResponse setType(String type) {
        this.type = type;
        return this;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public LoginResponse setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public LoginResponse setUsername(String username) {
        this.username = username;
        return this;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public LoginResponse setCustomerId(Long customerId) {
        this.customerId = customerId;
        return this;
    }
}
