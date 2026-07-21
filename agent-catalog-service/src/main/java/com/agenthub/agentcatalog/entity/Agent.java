package com.agenthub.agentcatalog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.genai.Chat;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agents")
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String version;
    private String agentType;
    private String provider;
    private String status;
    private String sessionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Transient
    @JsonIgnore
    private Chat chatSession;

    public Agent() {
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

    public String getName() {
        return name;
    }

    public Agent setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Agent setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Agent setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getAgentType() {
        return agentType;
    }

    public Agent setAgentType(String agentType) {
        this.agentType = agentType;
        return this;
    }

    public void setSessionId(String sessionId) {this.sessionId = sessionId;}


    public String getProvider() {
        return provider;
    }

    public Agent setProvider(String provider) {
        this.provider = provider;
        return this;
    }

    public void setChatSession(Chat chatSession) {
        this.chatSession = chatSession;
    }
    public Chat getChatSession() {return chatSession;}

    public String getStatus() {
        return status;
    }

    public Agent setStatus(String status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }


    public String getSessionId() {return sessionId;}
}