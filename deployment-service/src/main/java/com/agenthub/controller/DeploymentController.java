package com.agenthub.deployment.controller;

import com.agenthub.deployment.entity.Deployment;
import com.agenthub.deployment.service.DeploymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deployments")
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @GetMapping
    public List<Deployment> getAllDeployments() {
        return deploymentService.findAll();
    }

    @GetMapping("/{id}")
    public Deployment getDeploymentById(@PathVariable Long id) {
        return deploymentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Deployment createDeployment(@RequestBody Deployment deployment) {
        return deploymentService.create(deployment);
    }

    @PutMapping("/{id}")
    public Deployment updateDeployment(@PathVariable Long id, @RequestBody Deployment deployment) {
        return deploymentService.update(id, deployment);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeployment(@PathVariable Long id) {
        deploymentService.delete(id);
    }
}