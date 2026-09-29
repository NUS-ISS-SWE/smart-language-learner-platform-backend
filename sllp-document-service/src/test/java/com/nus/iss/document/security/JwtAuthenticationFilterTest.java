package com.nus.iss.document.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenAuthenticatesTheRequest() throws Exception {
        when(jwtProvider.validateToken("good")).thenReturn(true);
        when(jwtProvider.getUserIdFromToken("good")).thenReturn("42");
        when(jwtProvider.getEmailFromToken("good")).thenReturn("alex@example.com");
        when(jwtProvider.getRoleFromToken("good")).thenReturn("USER_BASIC");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("Bearer good"), new MockHttpServletResponse(), chain);

        JwtAuthenticationToken auth = assertInstanceOf(JwtAuthenticationToken.class,
                SecurityContextHolder.getContext().getAuthentication());
        assertEquals("42", auth.getUserId());
        assertEquals("alex@example.com", auth.getEmail());
        assertEquals("USER_BASIC", auth.getRole());
        assertNotNull(chain.getRequest(), "request must continue down the chain");
    }

    @Test
    void requestWithoutAuthorizationHeaderStaysAnonymous() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request(null), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
        verify(jwtProvider, never()).validateToken(any());
    }

    @Test
    void nonBearerSchemeIsIgnored() throws Exception {
        filter.doFilter(request("Basic YWxleDpzZWNyZXQ="), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtProvider, never()).validateToken(any());
    }

    @Test
    void invalidTokenStaysAnonymous() throws Exception {
        when(jwtProvider.validateToken("bad")).thenReturn(false);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("Bearer bad"), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void tokenWithoutUserIdStaysAnonymousInsteadOfFailingTheRequest() throws Exception {
        when(jwtProvider.validateToken("no-user-id")).thenReturn(true);
        when(jwtProvider.getUserIdFromToken("no-user-id")).thenThrow(new IllegalArgumentException("JWT is missing userId claim"));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("Bearer no-user-id"), new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        assertNotNull(chain.getRequest());
    }

    private static MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/documents");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
