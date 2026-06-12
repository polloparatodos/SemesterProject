package com.agenthub.deployment.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deployments")
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long agentId;
    private Long customerId;
    private String deploymentName;
    private String environmentName;
    private String region;
    private String status;
    private String requestedBy;

    private LocalDateTime requestedAt;
    private LocalDateTime updatedAt;

    public Deployment() {
    }

    @PrePersist
    public void prePersist() {
        requestedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAgentId() {
        return agentId;
    }

    public Deployment setAgentId(Long agentId) {
        this.agentId = agentId;
        return this;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Deployment setCustomerId(Long customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public Deployment setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
        return this;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public Deployment setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public Deployment setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public Deployment setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public Deployment setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
        return this;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}