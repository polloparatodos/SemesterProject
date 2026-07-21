package com.agenthub.agentcatalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Propagate the caller's JWT to downstream services (e.g. chat-service),
        // which reject unauthenticated requests with 403.
        restTemplate.getInterceptors().add((request, body, execution) -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null && !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                String authHeader = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, authHeader);
                }
            }
            return execution.execute(request, body);
        });

        return restTemplate;
    }
}
