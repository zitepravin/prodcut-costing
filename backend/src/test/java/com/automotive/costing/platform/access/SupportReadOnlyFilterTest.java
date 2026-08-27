package com.automotive.costing.platform.access;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupportReadOnlyFilterTest {

    @AfterEach
    void clear() {
        CustomerContext.clear();
    }

    @Test
    void blocksPostWhenSupportReadOnly() throws Exception {
        CustomerContext.setCustomerId("c1");
        CustomerContext.setSupportReadOnly(true);
        SupportReadOnlyFilter filter = new SupportReadOnlyFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getMethod()).thenReturn("POST");
        when(req.getRequestURI()).thenReturn("/api/v1/bom/nodes");
        StringWriter sw = new StringWriter();
        when(res.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(req, res, chain);

        verify(res).setStatus(403);
        verify(chain, never()).doFilter(any(), any());
        assertTrue(sw.toString().contains("SUPPORT-03"));
    }

    @Test
    void allowsGetWhenSupportReadOnly() throws Exception {
        CustomerContext.setSupportReadOnly(true);
        SupportReadOnlyFilter filter = new SupportReadOnlyFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/api/v1/bom/nodes");

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        verify(res, never()).setStatus(403);
        verify(res).setHeader(SupportReadOnlyFilter.SUPPORT_RO_HEADER, "true");
    }

    @Test
    void setsSupportHeaderOnGet() throws Exception {
        CustomerContext.setSupportReadOnly(true);
        SupportReadOnlyFilter filter = new SupportReadOnlyFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/api/v1/materials");

        filter.doFilterInternal(req, res, chain);

        verify(res).setHeader("X-Support-Read-Only", "true");
        verify(chain).doFilter(req, res);
    }

    @Test
    void noHeaderWhenNotSupportSession() throws Exception {
        CustomerContext.clear();
        SupportReadOnlyFilter filter = new SupportReadOnlyFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/api/v1/materials");

        filter.doFilterInternal(req, res, chain);

        verify(res, never()).setHeader(eq(SupportReadOnlyFilter.SUPPORT_RO_HEADER), anyString());
        verify(chain).doFilter(req, res);
    }

    @Test
    void blocksPostToPurchaseOrdersWhenSupportReadOnly() throws Exception {
        CustomerContext.setCustomerId("c1");
        CustomerContext.setSupportReadOnly(true);
        SupportReadOnlyFilter filter = new SupportReadOnlyFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getMethod()).thenReturn("POST");
        when(req.getRequestURI()).thenReturn("/api/v1/purchase-orders");
        StringWriter sw = new StringWriter();
        when(res.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(req, res, chain);

        verify(res).setStatus(403);
        verify(chain, never()).doFilter(any(), any());
        assertTrue(sw.toString().contains("SUPPORT-03"));
    }

    @Test
    void blocksPostToPurchaseOrderIssueWhenSupportReadOnly() throws Exception {
        CustomerContext.setSupportReadOnly(true);
        SupportReadOnlyFilter filter = new SupportReadOnlyFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getMethod()).thenReturn("POST");
        when(req.getRequestURI()).thenReturn("/api/v1/purchase-orders/po-1/issue");
        StringWriter sw = new StringWriter();
        when(res.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(req, res, chain);

        verify(res).setStatus(403);
        verify(chain, never()).doFilter(any(), any());
    }

}
