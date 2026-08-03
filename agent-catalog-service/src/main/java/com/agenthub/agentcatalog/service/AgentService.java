package com.agenthub.agentcatalog.service;

import com.agenthub.agentcatalog.entity.Agent;
import com.agenthub.agentcatalog.repository.AgentRepository;

import org.springframework.stereotype.Service;
import com.google.genai.Client;
import com.google.genai.Chat;
import com.google.genai.types.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final MessagingService messagingService;
    private Client client;

    public AgentService(AgentRepository agentRepository, MessagingService messagingService) {
        this.agentRepository = agentRepository;
        this.messagingService = messagingService;
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

        // Publish session initialization event to Kafka
        messagingService.publishSessionInitialization(
                savedAgent.getSessionId(),
                savedAgent.getName(),
                savedAgent.getDescription(),
                savedAgent.getId().toString()
        );

        return savedAgent;
    }

    public Agent loadExistingAgentSession(String sessionId) {

        Agent agent = agentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Agent not found with sessionId: " + sessionId));

        // Use the sessionId to retrieve chat history from chat-service
        List<Map<String, Object>> chatHistory = messagingService.getChatHistory(sessionId);

        // Convert chat history to formatted string
        String formattedHistory = messagingService.formatChatHistoryAsString(chatHistory);

        // Create system prompt that instructs Gemini to continue the conversation
        String sessionRestorePrompt = messagingService.buildSessionRestorePrompt(formattedHistory);

        // load message into new agent chat session and continue previous session
        Chat chatSession = createAgentSession();

        // Send system prompt with chat history to restore context
        chatSession.sendMessage(sessionRestorePrompt);

        //Store chatSession to agent entity in non-persistent variable
        agent.setChatSession(chatSession);

        return agent;
    }

    public Chat createAgentSession(){
        String modelName = "gemini-flash-latest";
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

        // Publish user message to Kafka
        messagingService.publishMessage(sessionId, "user", message, "text", agent.getDescription(), agentId.toString());

        // Send message to agent's Chat session and get response
        try {
            Chat chatSession = agent.getChatSession();

            // If chatSession is null, restore it from chat history
            if (chatSession == null) {
                agent = loadExistingAgentSession(sessionId);
                chatSession = agent.getChatSession();
            }

            GenerateContentResponse agentResponse = chatSession.sendMessage(message);

            // Publish agent response to Kafka
            String agentResponseText = agentResponse.text();
            return messagingService.publishMessage(sessionId, "agent", agentResponseText, "text", agent.getDescription(), agentId.toString());
        }
        catch (RuntimeException e) {
            throw new RuntimeException("Failed to send message to agent's chat session: " + e.getMessage(), e);
        }

    }

}