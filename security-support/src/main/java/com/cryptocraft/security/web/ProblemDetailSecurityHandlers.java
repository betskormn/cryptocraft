package com.cryptocraft.security.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * 401 and 403 as RFC 9457 ProblemDetail, like every other error of the API.
 */
public final class ProblemDetailSecurityHandlers {

    private ProblemDetailSecurityHandlers() {
    }

    public static AuthenticationEntryPoint unauthorized() {
        return (request, response, ex) -> {
            response.setHeader("WWW-Authenticate", "Bearer");
            write(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Sign in to continue");
        };
    }

    public static AccessDeniedHandler forbidden() {
        return (request, response, ex) ->
                write(response, HttpStatus.FORBIDDEN, "Forbidden", "Your role in this venue does not allow this");
    }

    private static void write(HttpServletResponse response, HttpStatus status, String title, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"title":"%s","status":%d,"detail":"%s"}""".formatted(title, status.value(), detail));
    }
}
