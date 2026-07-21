package com.agenthub.agentcatalog.controller;

import com.agenthub.agentcatalog.entity.Agent;
import com.agenthub.agentcatalog.service.AgentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public List<Agent> getAllAgents() {
        return agentService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Agent createAgent(@RequestBody Agent agent) {
        return agentService.createAgent(agent);
    }

    @GetMapping("/{id}")
    public Agent getAgentById(@PathVariable("id") Long id) {
        return agentService.findById(id);
    }

    @PutMapping("/{id}")
    public Agent updateAgent(@PathVariable("id") Long id, @RequestBody Agent agent) {
        return agentService.updateAgent(id, agent);
    }

    @PostMapping("/load-session")
    public Agent loadExistingAgentSession(@RequestParam("sessionId") String sessionId) {
        return agentService.loadExistingAgentSession(sessionId);
    }

    @PostMapping("/send-message")
    @ResponseStatus(HttpStatus.OK)
    public Map<String,Object> sendNewMessageToAgent(@RequestBody Map<String,Object> request) {
        Long agentId = ((Number) request.get("agentId")).longValue();
        String message = (String) request.get("message");
        return agentService.sendNewMessageToAgent(agentId, message);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAgent(@PathVariable("id") Long id) {
        agentService.deleteAgent(id);
    }
}