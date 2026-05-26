package com.experimentops.utils;

import com.experimentops.utils.constant.Headers;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

public final class HeaderUtil {

    private HeaderUtil() {}

    // ── Individual getters ───────────────────────────────────────

    public static String getRequestUuid(HttpServletRequest request) {
        return getHeader(request, Headers.REQUEST_UUID);
    }

    public static String getUserUuid(HttpServletRequest request) {
        return getHeader(request, Headers.X_TOKEN_C_USER_UUID);
    }

    public static String getUserName(HttpServletRequest request) {
        return getHeader(request, Headers.X_TOKEN_C_USER_NAME);
    }

    public static String getUserRole(HttpServletRequest request) {
        return getHeader(request, Headers.X_TOKEN_C_USER_ROLE);
    }

    public static String getWorkspaceUuid(HttpServletRequest request) {
        return getHeader(request, Headers.X_TOKEN_C_WORKSPACE_UUID);
    }

    // ── Full headers object ──────────────────────────────────────

    public static ExperimentOpsHeaders getHeaders(HttpServletRequest request) {
        ExperimentOpsHeaders headers = new ExperimentOpsHeaders();
        headers.setRequestUuid(getHeader(request, Headers.REQUEST_UUID));
        headers.setUserUuid(getHeader(request, Headers.X_TOKEN_C_USER_UUID));
        headers.setUserName(getHeader(request, Headers.X_TOKEN_C_USER_NAME));
        headers.setUserRole(getHeader(request, Headers.X_TOKEN_C_USER_ROLE));
        headers.setWorkspaceUuid(getHeader(request, Headers.X_TOKEN_C_WORKSPACE_UUID));
        headers.setAuthorizationToken(getHeader(request, Headers.AUTHORIZATION));
        headers.setRequestTimestamp(getHeader(request, Headers.X_REQUEST_TIMESTAMP));
        headers.setUserAgent(request.getHeader("User-Agent"));
        headers.setClientIp(resolveClientIp(request));
        headers.setInternalTraceUuid(getHeader(request, Headers.X_TRACE_UUID));

        return headers;
    }


    public static ExperimentOpsHeaders createHeadersForLogs(String requestUuid, String userUuid, String workspaceUuid) {
        ExperimentOpsHeaders headers = new ExperimentOpsHeaders();
        headers.setRequestUuid(StringUtils.defaultIfBlank(requestUuid, "N/A"));
        headers.setUserUuid(StringUtils.defaultIfBlank(userUuid, "N/A"));
        headers.setWorkspaceUuid(StringUtils.defaultIfBlank(workspaceUuid, "N/A"));
        return headers;
    }


    private static String getHeader(HttpServletRequest request, String headerName) {
        return request.getHeader(headerName);
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(ip)) {
            return ip.split(",")[0].trim(); // X-Forwarded-For can have multiple IPs, first is the real client
        }
        return request.getRemoteAddr();
    }
}