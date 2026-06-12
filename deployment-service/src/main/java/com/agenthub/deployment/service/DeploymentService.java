package com.agenthub.deployment.service;

import com.agenthub.deployment.entity.Deployment;
import com.agenthub.deployment.repository.DeploymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;

    public DeploymentService(DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    public List<Deployment> findAll() {
        return deploymentRepository.findAll();
    }

    public Deployment findById(Long id) {
        return deploymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deployment not found with id: " + id));
    }

    public Deployment create(Deployment deployment) {
        return deploymentRepository.save(deployment);
    }

    public Deployment update(Long id, Deployment updatedDeployment) {
        Deployment existingDeployment = findById(id);

        existingDeployment
                .setAgentId(updatedDeployment.getAgentId())
                .setCustomerId(updatedDeployment.getCustomerId())
                .setDeploymentName(updatedDeployment.getDeploymentName())
                .setEnvironmentName(updatedDeployment.getEnvironmentName())
                .setRegion(updatedDeployment.getRegion())
                .setStatus(updatedDeployment.getStatus())
                .setRequestedBy(updatedDeployment.getRequestedBy());

        return deploymentRepository.save(existingDeployment);
    }

    public void delete(Long id) {
        Deployment existingDeployment = findById(id);
        deploymentRepository.delete(existingDeployment);
    }
}