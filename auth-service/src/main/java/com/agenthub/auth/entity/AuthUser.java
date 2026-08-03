package com.agenthub.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_users")
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private Long customerId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean accountLocked = false;

    @Column(nullable = false)
    private Integer failedLoginAttempts = 0;

    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public AuthUser() {
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enabled == null) {
            enabled = true;
        }
        if (accountLocked == null) {
            accountLocked = false;
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public AuthUser setCustomerId(Long customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public AuthUser setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public AuthUser setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AuthUser setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public AuthUser setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getAccountLocked() {
        return accountLocked;
    }

    public AuthUser setAccountLocked(Boolean accountLocked) {
        this.accountLocked = accountLocked;
        return this;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public AuthUser setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
        return this;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public AuthUser setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
