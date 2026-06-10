package com.agenthub.agentcatalog.controller;

import com.agenthub.agentcatalog.entity.Agent;
import com.agenthub.agentcatalog.service.AgentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{id}")
    public Agent getAgentById(@PathVariable Long id) {
        return agentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Agent createAgent(@RequestBody Agent agent) {
        return agentService.create(agent);
    }

    @PutMapping("/{id}")
    public Agent updateAgent(@PathVariable Long id, @RequestBody Agent agent) {
        return agentService.update(id, agent);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAgent(@PathVariable Long id) {
        agentService.delete(id);
    }
}