package com.agenthub.chat.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String sessionId;

    private String sender;  // "user" or "agent"
    private String content;
    private String messageType;  // "text", "system", "error", etc.
    private String chatDescription;

    @Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String agentId;  // Link to Agent entity
    private String metadata;  // JSON string for additional data

    public ChatMessage() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and fluent setters
    public String getId() { return id; }

    public String getSessionId() { return sessionId; }
    public ChatMessage setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public String getSender() { return sender; }
    public ChatMessage setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public String getContent() { return content; }
    public ChatMessage setContent(String content) {
        this.content = content;
        return this;
    }

    public String getMessageType() { return messageType; }
    public ChatMessage setMessageType(String messageType) {
        this.messageType = messageType;
        return this;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getAgentId() { return agentId; }
    public ChatMessage setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    public String getMetadata() { return metadata; }
    public ChatMessage setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    public String getChatDescription() { return chatDescription; }
    public ChatMessage setChatDescription(String chatDescription) {
        this.chatDescription = chatDescription;
        return this;
    }
}
