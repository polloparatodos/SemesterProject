package com.agenthub.auth.dto;

public class ForgotPasswordResponse {

    private String resetToken;
    private String expiresAt;
    private String message;

    public ForgotPasswordResponse() {
    }

    public ForgotPasswordResponse(String resetToken, String expiresAt, String message) {
        this.resetToken = resetToken;
        this.expiresAt = expiresAt;
        this.message = message;
    }

    public String getResetToken() {
        return resetToken;
    }

    public ForgotPasswordResponse setResetToken(String resetToken) {
        this.resetToken = resetToken;
        return this;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public ForgotPasswordResponse setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ForgotPasswordResponse setMessage(String message) {
        this.message = message;
        return this;
    }
}
