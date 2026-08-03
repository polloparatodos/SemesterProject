package com.agenthub.chat.listener;

import com.agenthub.chat.entity.ChatMessage;
import com.agenthub.chat.service.ChatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer that listens to chat-related topics and persists messages to MongoDB.
 */
@Component
public class KafkaMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageListener.class);

    private final ChatMessageService chatMessageService;

    public KafkaMessageListener(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    /**
     * Listens to chat.messages topic and persists user/agent messages to MongoDB.
     *
     * @param messageData Map containing message data from Kafka
     */
    @KafkaListener(topics = "${kafka.topics.chat-messages:chat.messages}", groupId = "chat-service-group")
    public void consumeChatMessage(Map<String, Object> messageData) {
        try {
            logger.info("Received chat message from Kafka: sessionId={}, sender={}",
                messageData.get("sessionId"), messageData.get("sender"));

            ChatMessage chatMessage = new ChatMessage()
                .setSessionId((String) messageData.get("sessionId"))
                .setSender((String) messageData.get("sender"))
                .setContent((String) messageData.get("content"))
                .setMessageType((String) messageData.get("messageType"))
                .setChatDescription((String) messageData.get("chatDescription"))
                .setAgentId((String) messageData.get("agentId"));

            chatMessageService.save(chatMessage);

            logger.info("Successfully persisted chat message to MongoDB: id={}", chatMessage.getId());

        } catch (Exception e) {
            logger.error("Failed to process chat message from Kafka: {}", e.getMessage(), e);
            // In production, consider implementing retry logic or dead letter queue
        }
    }

    /**
     * Listens to chat.session.events topic and persists session events to MongoDB.
     *
     * @param eventData Map containing session event data from Kafka
     */
    @KafkaListener(topics = "${kafka.topics.session-events:chat.session.events}", groupId = "chat-service-group")
    public void consumeSessionEvent(Map<String, Object> eventData) {
        try {
            logger.info("Received session event from Kafka: sessionId={}, eventType={}",
                eventData.get("sessionId"), eventData.get("eventType"));

            String eventType = (String) eventData.get("eventType");
            String sessionId = (String) eventData.get("sessionId");
            String agentName = (String) eventData.get("agentName");
            String chatDescription = (String) eventData.get("chatDescription");
            String agentId = (String) eventData.get("agentId");

            // Create a system message for session events
            ChatMessage systemMessage = new ChatMessage()
                .setSessionId(sessionId)
                .setSender("system")
                .setContent(buildEventMessage(eventType, agentName))
                .setMessageType("system")
                .setChatDescription(chatDescription)
                .setAgentId(agentId);

            chatMessageService.save(systemMessage);

            logger.info("Successfully persisted session event to MongoDB: id={}", systemMessage.getId());

        } catch (Exception e) {
            logger.error("Failed to process session event from Kafka: {}", e.getMessage(), e);
        }
    }

    /**
     * Builds a human-readable message for session events.
     *
     * @param eventType Type of session event
     * @param agentName Name of the agent
     * @return Formatted event message
     */
    private String buildEventMessage(String eventType, String agentName) {
        return switch (eventType) {
            case "initialized" -> "Chat session initialized for agent: " + agentName;
            case "terminated" -> "Chat session terminated for agent: " + agentName;
            case "resumed" -> "Chat session resumed for agent: " + agentName;
            default -> "Session event: " + eventType + " for agent: " + agentName;
        };
    }
}
