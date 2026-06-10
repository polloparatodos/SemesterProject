package com.agenthub.deployment.repository;

import com.agenthub.deployment.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
}