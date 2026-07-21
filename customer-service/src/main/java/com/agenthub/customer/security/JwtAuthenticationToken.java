package com.agenthub.customer.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private final Long customerId;

    public JwtAuthenticationToken(String principal, Long customerId, Collection<? extends GrantedAuthority> authorities) {
        super(principal, null, authorities);
        this.customerId = customerId;
    }

    public Long getCustomerId() {
        return customerId;
    }
}
