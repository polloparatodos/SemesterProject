package com.agenthub.customer.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtPublicKeyProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtPublicKeyProvider.class);
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 2000;

    private PublicKey publicKey;

    @Value("${auth-service.url:http://auth-service:8080}")
    private String authServiceUrl;

    @PostConstruct
    public void init() {
        fetchPublicKey();
    }

    private void fetchPublicKey() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("Fetching public key from auth-service (attempt {}/{})", attempt, MAX_RETRIES);
                RestTemplate restTemplate = new RestTemplate();
                String url = authServiceUrl + "/api/auth/public-key";

                @SuppressWarnings("unchecked")
                Map<String, String> response = restTemplate.getForObject(url, Map.class);

                if (response != null && response.containsKey("publicKey")) {
                    String publicKeyString = response.get("publicKey");
                    this.publicKey = parsePublicKey(publicKeyString);
                    logger.info("Public key successfully fetched from auth-service");
                    return;
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch public key (attempt {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting to retry public key fetch", ie);
                    }
                }
            }
        }
        throw new RuntimeException("Failed to fetch public key from auth-service after " + MAX_RETRIES + " attempts");
    }

    private PublicKey parsePublicKey(String publicKeyString) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
