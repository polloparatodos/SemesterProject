package com.agenthub.auth.dto;

public class ResetPasswordRequest {

    private String resetToken;
    private String newPassword;

    public ResetPasswordRequest() {
    }

    public String getResetToken() {
        return resetToken;
    }

    public ResetPasswordRequest setResetToken(String resetToken) {
        this.resetToken = resetToken;
        return this;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public ResetPasswordRequest setNewPassword(String newPassword) {
        this.newPassword = newPassword;
        return this;
    }
}
