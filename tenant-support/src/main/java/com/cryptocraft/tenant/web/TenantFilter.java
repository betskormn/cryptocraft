package com.cryptocraft.tenant.web;

import com.cryptocraft.tenant.TenantContext;
import com.cryptocraft.tenant.TenantStatus;
import com.cryptocraft.tenant.TenantStatusLookup;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    private final TenantStatusLookup tenantStatusLookup;
    // path prefixes not scoped to a single tenant (tenant.free-paths)
    private final List<String> tenantFreePaths;

    public TenantFilter(TenantStatusLookup tenantStatusLookup, List<String> tenantFreePaths) {
        this.tenantStatusLookup = tenantStatusLookup;
        this.tenantFreePaths = List.copyOf(tenantFreePaths);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return tenantFreePaths.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Long tenantId = resolveTenantId(request);
        if (tenantId == null) {
            writeProblem(response, HttpStatus.BAD_REQUEST, "Tenant is required",
                    "Header %s must contain a positive tenant id".formatted(TENANT_HEADER));
            return;
        }
        Optional<TenantStatus> status = tenantStatusLookup.findStatus(tenantId);
        if (status.isEmpty()) {
            writeProblem(response, HttpStatus.NOT_FOUND, "Tenant not found",
                    "Tenant %d does not exist".formatted(tenantId));
            return;
        }
        if (status.get() == TenantStatus.SUSPENDED) {
            writeProblem(response, HttpStatus.FORBIDDEN, "Tenant is suspended",
                    "Tenant %d is suspended".formatted(tenantId));
            return;
        }
        try {
            TenantContext.set(tenantId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    // the venue being worked in; staff tokens are checked against it by security-support (VenueAccess)
    private Long resolveTenantId(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            long tenantId = Long.parseLong(header.trim());
            return tenantId > 0 ? tenantId : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status, String title, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"title":"%s","status":%d,"detail":"%s"}""".formatted(title, status.value(), detail));
    }
}
