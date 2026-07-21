package com.agenthub.agentcatalog.service;

import com.agenthub.agentcatalog.entity.Agent;
import com.agenthub.agentcatalog.repository.AgentRepository;

import org.springframework.stereotype.Service;
import com.google.genai.Client;
import com.google.genai.Chat;
import com.google.genai.types.*;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final RestTemplate restTemplate;
    private Client client;
    private final String chatServiceUrl = "http://chat-service:8084/api/messages";

    public AgentService(AgentRepository agentRepository, RestTemplate restTemplate) {
        this.agentRepository = agentRepository;
        this.restTemplate = restTemplate;
    }

    private Client getClient() {
        if (client == null) {
            try {
                client = new Client();
            } catch (Exception e) {
                System.err.println("Failed to initialize Google GenAI Client. API key may be missing: " + e.getMessage());
                throw new RuntimeException("Google GenAI Client initialization failed", e);
            }
        }
        return client;
    }

    public List<Agent> findAll() {
        return agentRepository.findAll();
    }

    public Agent findById(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + id));
    }

    public Agent createAgent(Agent agent) {
        Chat chatSession = createAgentSession();
        generateChatSessionId(agent);
        Agent savedAgent = agentRepository.save(agent);

        // Set the chat session on the agent (transient field, not persisted)
        savedAgent.setChatSession(chatSession);

        // Persist session initialization to chat-service
        persistSessionInitialization(savedAgent);

        return savedAgent;
    }

    public Agent loadExistingAgentSession(String sessionId) {

        Agent agent = agentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Agent not found with sessionId: " + sessionId));

        // Use the sessionId to retrieve chat history from chat-service
        List<Map<String, Object>> chatHistory = getChatHistory(sessionId);

        // Convert chat history to formatted string
        String formattedHistory = formatChatHistoryAsString(chatHistory);

        // Create system prompt that instructs Gemini to continue the conversation
        String sessionRestorePrompt = buildSessionRestorePrompt(formattedHistory);

        // load message into new agent chat session and continue previous session
        Chat chatSession = createAgentSession();

        // Send system prompt with chat history to restore context
        chatSession.sendMessage(sessionRestorePrompt);

        //Store chatSession to agent entity in non-persistent variable
        agent.setChatSession(chatSession);

        return agent;
    }

    private List<Map<String, Object>> getChatHistory(String sessionId) {
        try {
            String url = chatServiceUrl + "/session/" + sessionId;
            Map<String, Object>[] messages = restTemplate.getForObject(url, Map[].class);
            return messages != null ? Arrays.asList(messages) : List.of();
        } catch (Exception e) {
            System.err.println("Failed to retrieve chat history from chat-service: " + e.getMessage());
            return List.of();
        }
    }

    private String formatChatHistoryAsString(List<Map<String, Object>> chatHistory) {
        if (chatHistory.isEmpty()) {
            return "";
        }

        StringBuilder formattedChat = new StringBuilder();

        for (Map<String, Object> message : chatHistory) {
            String sender = (String) message.get("sender");
            String content = (String) message.get("content");
            String messageType = (String) message.get("messageType");

            if (sender != null && content != null) {
                // Skip system messages or include them with special formatting
                if ("system".equals(messageType)) {
                    continue; // Skip system initialization messages
                }

                // Capitalize sender label (user -> User, agent -> Agent)
                String label = sender.substring(0, 1).toUpperCase() + sender.substring(1);
                formattedChat.append(label).append(": ").append(content).append("\n");
            }
        }

        return formattedChat.toString().trim();
    }

    private String buildSessionRestorePrompt(String formattedHistory) {
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

    public Chat createAgentSession(){//private final String modelName = "gemini-3.5-flash";
        String modelName = "gemini-2.5-flash";
        return getClient().chats.create(modelName);}

    private void generateChatSessionId(Agent agent) {
        String sessionId = UUID.randomUUID().toString();
        agent.setSessionId(sessionId);
    }

    public Agent updateAgent(Long id, Agent updatedAgent) {
        Agent existingAgent = findById(id);

        existingAgent
                .setName(updatedAgent.getName())
                .setDescription(updatedAgent.getDescription())
                .setVersion(updatedAgent.getVersion())
                .setAgentType(updatedAgent.getAgentType())
                .setProvider(updatedAgent.getProvider())
                .setStatus(updatedAgent.getStatus());

        return agentRepository.save(existingAgent);
    }

    public void deleteAgent(Long id) {
        Agent existingAgent = findById(id);
        agentRepository.delete(existingAgent);
        //TODO: Optionally, you can also delete the chat session from chat-service if needed.
    }

    public Map<String,Object> sendNewMessageToAgent(Long agentId, String message) {

        // Get the agent to retrieve sessionId
        Agent agent = findById(agentId);
        if(agent == null){
            throw new RuntimeException("Agent not found with id: " + agentId);
        }
        String sessionId = agent.getSessionId();
        if (sessionId == null) {
            throw new RuntimeException("Agent does not have an active session");
        }

        // Persist user message to chat-service
        persistChatMessage(sessionId, agent.getDescription(),"user", message, agentId.toString());

        // Send message to agent's Chat session and get response
        try {
            Chat chatSession = agent.getChatSession();

            // If chatSession is null, restore it from chat history
            if (chatSession == null) {
                agent = loadExistingAgentSession(sessionId);
                chatSession = agent.getChatSession();
            }

            GenerateContentResponse agentResponse = chatSession.sendMessage(message);

            // Persisting agent response to chat-service
            String agentResponseText = agentResponse.text();
            return persistChatMessage(sessionId, agent.getDescription(),"agent", agentResponseText, agentId.toString());
        }
        catch (RuntimeException e) {
            throw new RuntimeException("Failed to send message to agent's chat session: " + e.getMessage(), e);
        }

    }

    private Map<String,Object> persistChatMessage(String sessionId, String chatDescription, String sender, String content, String agentId) {
        try {

            Map<String, Object> message = new HashMap<>();
            message.put("sessionId", sessionId);
            message.put("sender", sender);
            message.put("content", content);
            message.put("messageType", "text");
            message.put("chatDescription", chatDescription);
            message.put("agentId", agentId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(message, headers);

            restTemplate.postForObject(chatServiceUrl, request, Map.class);

            return message;

        } catch (Exception e) {
            System.err.println("Failed to persist message to chat-service: " + e.getMessage());
            throw new RuntimeException("Failed to persist chat message", e);
        }

    }

    private void persistSessionInitialization(Agent agent) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("sessionId", agent.getSessionId());
            message.put("sender", "system");
            message.put("content", "Chat session initialized for agent: " + agent.getName());
            message.put("messageType", "system");
            message.put("chatDescription", agent.getDescription());
            message.put("agentId", agent.getId().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(message, headers);

            restTemplate.postForObject(chatServiceUrl, request, Map.class);
        } catch (Exception e) {
            // Log error but don't fail agent creation
            System.err.println("Failed to persist session to chat-service: " + e.getMessage());
        }
    }
}