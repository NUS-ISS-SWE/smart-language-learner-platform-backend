package com.nus.iss.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAuditHttpFilterTest {

    @Mock
    private AdminAuditRecorder recorder;

    @Test
    void successfulAdminRequestIsRecorded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/metrics");
        request.addHeader("X-Sllp-Username", "alextan");
        request.addHeader("X-Sllp-User-Id", "17");
        request.addHeader("X-Sllp-Request-Id", "req-1");
        MockFilterChain chain = new MockFilterChain();

        new AdminAuditHttpFilter(recorder).doFilter(request, response(200), chain);

        assertNotNull(chain.getRequest(), "request must reach the controller");
        verify(recorder).record("ADMIN_RESOURCE_VIEWED", "alextan", "17", "HTTP_RESOURCE", "/api/v1/admin/metrics",
                "req-1", "GET", "/api/v1/admin/metrics");
    }

    @Test
    void failedAdminRequestIsNotRecorded() throws Exception {
        new AdminAuditHttpFilter(recorder).doFilter(
                new MockHttpServletRequest("GET", "/api/v1/admin/metrics"), response(403), new MockFilterChain());

        verifyNothingRecorded();
    }

    @Test
    void nonAdminPathIsNotRecorded() throws Exception {
        new AdminAuditHttpFilter(recorder).doFilter(
                new MockHttpServletRequest("GET", "/api/v1/rubrics"), response(200), new MockFilterChain());

        verifyNothingRecorded();
    }

    @Test
    void auditIngestionItselfIsNeverRecorded() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        new AdminAuditHttpFilter(recorder).doFilter(
                new MockHttpServletRequest("POST", "/internal/audit-events"), response(201), chain);

        assertNotNull(chain.getRequest());
        verifyNothingRecorded();
    }

    private static MockHttpServletResponse response(int status) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);
        return response;
    }

    private void verifyNothingRecorded() {
        verify(recorder, never()).record(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
