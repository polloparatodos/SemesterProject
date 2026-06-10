package com.agenthub.agentcatalog.repository;

import com.agenthub.agentcatalog.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<Agent, Long> {
}