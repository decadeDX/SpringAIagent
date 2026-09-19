package io.github.decadedx.springaiagent.common;

import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestResponseInfrastructureTest {

    @AfterEach
    void clearRequestContext() {
        RequestIdContext.clear();
    }

    @Test
    void shouldReuseValidRequestIdAndExposeItToResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, "req-demo-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdSeenInChain = new AtomicReference<>();

        new RequestIdFilter().doFilter(request, response,
                (servletRequest, servletResponse) -> requestIdSeenInChain.set(RequestIdContext.currentOrCreate()));

        assertEquals("req-demo-001", requestIdSeenInChain.get());
        assertEquals("req-demo-001", response.getHeader(RequestIdFilter.HEADER_NAME));
    }

    @Test
    void shouldReplaceUnsafeRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, "unsafe\r\nvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestIdFilter().doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertNotEquals("unsafe\r\nvalue", response.getHeader(RequestIdFilter.HEADER_NAME));
    }

    @Test
    void shouldReturnStableBusinessErrorShape() {
        RequestIdContext.set("req-error-001");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Result<Void>> response = handler.handleBusinessException(
                new BusinessException(HttpStatus.CONFLICT, ApiCode.BUSINESS_CONFLICT, "状态冲突"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(40900, response.getBody().code());
        assertEquals("req-error-001", response.getBody().requestId());
        assertNull(response.getBody().data());
    }
}
