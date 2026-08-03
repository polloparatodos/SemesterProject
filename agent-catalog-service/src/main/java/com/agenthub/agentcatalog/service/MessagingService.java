package com.agenthub.agentcatalog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for all messaging operations.
 * Uses Kafka for asynchronous message publishing and HTTP for queries.
 */
@Service
public class MessagingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final String chatServiceUrl;
    private final String chatMessagesTopic;
    private final String sessionEventsTopic;

    public MessagingService(KafkaTemplate<String, Object> kafkaTemplate,
                            RestTemplate restTemplate,
                            @Value("${chat.service.url:http://chat-service:8084/api/messages}") String chatServiceUrl,
                            @Value("${kafka.topics.chat-messages:chat.messages}") String chatMessagesTopic,
                            @Value("${kafka.topics.session-events:chat.session.events}") String sessionEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
        this.chatServiceUrl = chatServiceUrl;
        this.chatMessagesTopic = chatMessagesTopic;
        this.sessionEventsTopic = sessionEventsTopic;
    }

    /**
     * Publishes a message to Kafka for persistence by chat-service.
     *
     * @param sessionId Session identifier
     * @param sender Message sender (user, agent, system)
     * @param content Message content
     * @param messageType Type of message (text, system)
     * @param chatDescription Description of the chat session
     * @param agentId Agent identifier
     * @return The message map that was published
     */
    public Map<String, Object> publishMessage(String sessionId,
                                               String sender,
                                               String content,
                                               String messageType,
                                               String chatDescription,
                                               String agentId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("sessionId", sessionId);
            message.put("sender", sender);
            message.put("content", content);
            message.put("messageType", messageType);
            message.put("chatDescription", chatDescription);
            message.put("agentId", agentId);

            // Use sessionId as the key for partitioning - ensures messages for same session go to same partition
            kafkaTemplate.send(chatMessagesTopic, sessionId, message);

            return message;

        } catch (Exception e) {
            System.err.println("Failed to publish message to Kafka: " + e.getMessage());
            throw new RuntimeException("Failed to publish chat message", e);
        }
    }

    /**
     * Publishes a session event to Kafka.
     * Used for session lifecycle events (initialization, termination, etc.)
     *
     * @param sessionId Session identifier
     * @param eventType Type of session event (initialized, terminated, etc.)
     * @param agentName Name of the agent
     * @param chatDescription Description of the chat session
     * @param agentId Agent identifier
     */
    public void publishSessionEvent(String sessionId,
                                     String eventType,
                                     String agentName,
                                     String chatDescription,
                                     String agentId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("sessionId", sessionId);
            event.put("eventType", eventType);
            event.put("agentName", agentName);
            event.put("chatDescription", chatDescription);
            event.put("agentId", agentId);
            event.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send(sessionEventsTopic, sessionId, event);

        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to publish session event to Kafka: " + e.getMessage());
        }
    }

    /**
     * Retrieves chat history for a given session from the chat-service via HTTP.
     * HTTP is used here because this is a query operation, not an event.
     *
     * @param sessionId Session identifier
     * @return List of messages as Maps containing sender, content, and messageType
     */
    public List<Map<String, Object>> getChatHistory(String sessionId) {
        try {
            String url = chatServiceUrl + "/session/" + sessionId;
            Map<String, Object>[] messages = restTemplate.getForObject(url, Map[].class);
            return messages != null ? Arrays.asList(messages) : List.of();
        } catch (Exception e) {
            System.err.println("Failed to retrieve chat history from chat-service: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Formats chat history into a readable string format.
     * Filters out system messages and formats as "Sender: content" lines.
     *
     * @param chatHistory List of message maps
     * @return Formatted chat history as a string
     */
    public String formatChatHistoryAsString(List<Map<String, Object>> chatHistory) {
        if (chatHistory.isEmpty()) {
            return "";
        }

        StringBuilder formattedChat = new StringBuilder();

        for (Map<String, Object> message : chatHistory) {
            String sender = (String) message.get("sender");
            String content = (String) message.get("content");
            String messageType = (String) message.get("messageType");

            if (sender != null && content != null) {
                // Skip system messages
                if ("system".equals(messageType)) {
                    continue;
                }

                // Capitalize sender label (user -> User, agent -> Agent)
                String label = sender.substring(0, 1).toUpperCase() + sender.substring(1);
                formattedChat.append(label).append(": ").append(content).append("\n");
            }
        }

        return formattedChat.toString().trim();
    }

    /**
     * Builds a session restore prompt that includes the conversation history.
     * Used to restore context when reloading an agent session.
     *
     * @param formattedHistory The formatted conversation history
     * @return System prompt with embedded conversation history
     */
    public String buildSessionRestorePrompt(String formattedHistory) {
        return "SYSTEM INSTRUCTION: You are continuing a previous conversation. " +
                "Below is the complete conversation history between you (the Agent) and the User. " +
                "Read and understand this conversation as if you were actively participating in it. " +
                "After reviewing the history, continue the conversation naturally from where it left off. " +
                "Maintain context, remember all details discussed, and respond as the same Agent who was in this conversation.\n\n" +
                "--- CONVERSATION HISTORY ---\n" +
                formattedHistory +
                "\n--- END OF HISTORY ---\n\n" +
                "You are now ready to continue this conversation. The user may ask follow-up questions or continue the discussion. " +
                "Respond naturally based on the full context above.";
    }

    /**
     * Convenience method to publish a session initialization event.
     *
     * @param sessionId Session identifier
     * @param agentName Name of the agent
     * @param chatDescription Description of the chat session
     * @param agentId Agent identifier
     */
    public void publishSessionInitialization(String sessionId, String agentName, String chatDescription, String agentId) {
        publishSessionEvent(sessionId, "initialized", agentName, chatDescription, agentId);
    }
}
