package com.agenthub.customer.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String organizationName;
    private String contactName;
    private String email;
    private String subscriptionPlan;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Customer() {
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public Customer setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
        return this;
    }

    public String getContactName() {
        return contactName;
    }

    public Customer setContactName(String contactName) {
        this.contactName = contactName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Customer setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public Customer setSubscriptionPlan(String subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public Customer setStatus(String status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}