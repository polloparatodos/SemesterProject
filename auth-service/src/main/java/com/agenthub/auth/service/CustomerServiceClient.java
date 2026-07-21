package com.agenthub.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CustomerServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${customer-service.url}")
    private String customerServiceUrl;

    public CustomerServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validateCustomerExists(Long customerId) {
        try {
            String url = customerServiceUrl + "/api/customers/" + customerId;
            restTemplate.getForObject(url, Object.class);
            logger.info("Customer {} validated successfully", customerId);
            return true;
        } catch (Exception e) {
            logger.warn("Customer {} not found or validation failed: {}", customerId, e.getMessage());
            return false;
        }
    }
}
