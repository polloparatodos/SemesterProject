package com.agenthub.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_user_id", nullable = false)
    private AuthUser authUser;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean used = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PasswordResetToken() {
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (used == null) {
            used = false;
        }
    }

    public Long getId() {
        return id;
    }

    public AuthUser getAuthUser() {
        return authUser;
    }

    public PasswordResetToken setAuthUser(AuthUser authUser) {
        this.authUser = authUser;
        return this;
    }

    public String getToken() {
        return token;
    }

    public PasswordResetToken setToken(String token) {
        this.token = token;
        return this;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public PasswordResetToken setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public Boolean getUsed() {
        return used;
    }

    public PasswordResetToken setUsed(Boolean used) {
        this.used = used;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
