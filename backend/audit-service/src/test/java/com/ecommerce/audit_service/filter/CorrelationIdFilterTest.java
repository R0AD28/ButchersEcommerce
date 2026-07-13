package com.ecommerce.audit_service.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
    }

    @Test
    void filterKeepsProvidedCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "correlation-provided");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(
                "correlation-provided",
                request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE)
        );
        assertEquals(
                "correlation-provided",
                response.getHeader(CorrelationIdFilter.HEADER_NAME)
        );
    }

    @Test
    void filterGeneratesUuidWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String correlationId = (String) request.getAttribute(
                CorrelationIdFilter.REQUEST_ATTRIBUTE
        );
        assertNotNull(correlationId);
        assertDoesNotThrow(() -> UUID.fromString(correlationId));
        assertEquals(
                correlationId,
                response.getHeader(CorrelationIdFilter.HEADER_NAME)
        );
    }
}
