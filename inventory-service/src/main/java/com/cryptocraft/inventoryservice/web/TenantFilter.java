package com.cryptocraft.inventoryservice.web;

import com.cryptocraft.inventoryservice.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            Long tenantId = resolveTenantId(request);
            if (tenantId == null) {
                throw new IllegalStateException("Tenant is required");
            }
            TenantContext.set(tenantId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Long resolveTenantId(HttpServletRequest request) {
        // TODO: replace with JWT-based tenant
        return 1L;
    }
}