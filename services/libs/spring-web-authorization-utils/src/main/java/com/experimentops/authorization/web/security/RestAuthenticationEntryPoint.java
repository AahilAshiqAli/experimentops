package com.experimentops.authorization.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

// The benifit of this class is that it logs the rejected URI in debug level. And passes a msg in response
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        log.debug("Rejecting unauthenticated request {}: {}", request.getRequestURI(), exception.getMessage());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getLocalizedMessage());
    }
}
