package com.experimentops.authorization.web.security;

import com.experimentops.utils.JSONUtil;
import com.experimentops.utils.JwtUtil;
import com.experimentops.utils.constant.Headers;
import com.experimentops.utils.constant.RoleType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
// Runs once per every request
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(TokenAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";

    private final CustomUserDetailsService customUserDetailsService;

    // Need to add Login Path here.
    private final Set<String> pathsNotToFilter = new HashSet<>();
    private final Set<String> internalAuthenticatedUrls = new HashSet<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Check if role can be extracted from Token
        String roleName = fetchRoleName(request);
        if (StringUtils.isBlank(roleName)) {
            reject(response, HttpStatus.UNAUTHORIZED, "Missing or invalid bearer token role");
            log.debug("Rejecting request without role: {}", request.getRequestURI());
            return;
        }

        // Do we recognize this role If so, instantiate an empty UserDetail object for it
        UserDetails userDetails;
        try {
            userDetails = customUserDetailsService.loadUserByUsername(roleName);
        } catch (RuntimeException ex) {
            reject(response, HttpStatus.UNAUTHORIZED, "Invalid role: " + roleName);
            log.debug("Rejecting request with invalid role {} on {}", roleName, request.getRequestURI());
            return;
        }

        // This generates a token which defines what this role can do and sets authentication in spring security context
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String fetchRoleName(HttpServletRequest request) {
        if (uriInPath(request.getRequestURI(), internalAuthenticatedUrls)) {
            return RoleType.INTERNAL.getRoleName();
        }

        String gatewayRole = request.getHeader(Headers.X_TOKEN_C_USER_ROLE);
        if (StringUtils.isNotBlank(gatewayRole)) {
            return gatewayRole;
        }

        String authorization = request.getHeader(Headers.AUTHORIZATION);
        if (StringUtils.length(authorization) > BEARER.length() && StringUtils.startsWithIgnoreCase(authorization, BEARER)) {
            authorization = StringUtils.trimToNull(StringUtils.substring(authorization, BEARER.length()));
        }
        if (StringUtils.isBlank(authorization)) {
            return null;
        }

        Map<String, Object> claims = JwtUtil.parseToken(authorization);
        return JwtUtil.getClaim("role", claims, String.class);
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(JSONUtil.toNonTypedJsonFromObject(Map.of(
                "status", status.value(),
                "message", message
        )));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return uriInPath(request.getRequestURI(), pathsNotToFilter);
    }

    private boolean uriInPath(String uri, Set<String> paths) {
        for (String path : paths) {
            if (uri.contains(path) || Pattern.compile(path).matcher(uri).find()) {
                return true;
            }
        }
        return false;
    }

    public void excludePathFromFilter(String path) {
        pathsNotToFilter.add(path);
    }

    public void excludePathFromFilter(Set<String> paths) {
        pathsNotToFilter.addAll(paths);
    }

    public void internalAuthenticatedUrl(String path) {
        internalAuthenticatedUrls.add(path);
    }

    public void internalAuthenticatedUrls(Set<String> paths) {
        internalAuthenticatedUrls.addAll(paths);
    }
}
