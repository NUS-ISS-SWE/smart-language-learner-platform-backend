package com.nus.iss.document.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationTokenTest {

    @Test
    void exposesTheUserFromTheJwt() {
        JwtAuthenticationToken token = new JwtAuthenticationToken("42", "alex@example.com", "USER_BASIC");

        assertEquals("42", token.getPrincipal());
        assertEquals("42", token.getUserId());
        assertEquals("alex@example.com", token.getName());
        assertEquals("alex@example.com", token.getEmail());
        assertEquals("USER_BASIC", token.getRole());
        assertNull(token.getCredentials());
        assertNull(token.getDetails());
    }

    @Test
    void roleBecomesASpringAuthorityInUpperCase() {
        JwtAuthenticationToken token = new JwtAuthenticationToken("1", "admin@example.com", "admin");

        assertEquals("ROLE_ADMIN", token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .reduce((a, b) -> a + "," + b)
                .orElseThrow());
    }

    @Test
    void isAuthenticatedUntilRevoked() {
        JwtAuthenticationToken token = new JwtAuthenticationToken("42", "alex@example.com", "USER_BASIC");
        assertTrue(token.isAuthenticated());

        token.setAuthenticated(false);

        assertFalse(token.isAuthenticated());
    }
}
