package com.agenthub.agentcatalog.repository;

import com.agenthub.agentcatalog.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findBySessionId(String sessionId);
}