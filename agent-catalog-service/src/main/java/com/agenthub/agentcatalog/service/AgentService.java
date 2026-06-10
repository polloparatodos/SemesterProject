package com.agenthub.agentcatalog.service;

import com.agenthub.agentcatalog.entity.Agent;
import com.agenthub.agentcatalog.repository.AgentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentService {

    private final AgentRepository agentRepository;

    public AgentService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    public List<Agent> findAll() {
        return agentRepository.findAll();
    }

    public Agent findById(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + id));
    }

    public Agent create(Agent agent) {
        return agentRepository.save(agent);
    }

    public Agent update(Long id, Agent updatedAgent) {
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

    public void delete(Long id) {
        Agent existingAgent = findById(id);
        agentRepository.delete(existingAgent);
    }
}