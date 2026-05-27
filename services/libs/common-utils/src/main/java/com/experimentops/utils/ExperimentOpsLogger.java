package com.experimentops.utils;

import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

public class ExperimentOpsLogger {

    private final org.slf4j.Logger delegate;
    private static final String NA = "N/A";

    private ExperimentOpsLogger(Class<?> clazz) {
        delegate = LoggerFactory.getLogger(clazz);
    }

    public static ExperimentOpsLogger getLogger(Class<?> clazz) {
        return new ExperimentOpsLogger(clazz);
    }

    // ── public API ──────────────────────────────────────────────

    public void info(ExperimentOpsHeaders headers, String message) {
        delegate.info(format(headers, message));
    }

    public void warn(ExperimentOpsHeaders headers, String message) {
        delegate.warn(format(headers, message));
    }

    public void error(ExperimentOpsHeaders headers, String message) {
        delegate.error(format(headers, message));
    }

    public void error(ExperimentOpsHeaders headers, String message, Throwable th) {
        delegate.error(format(headers, message), th);
    }

    public void debug(ExperimentOpsHeaders headers, String message) {
        delegate.debug(format(headers, message));
    }

    // ── internals ───────────────────────────────────────────────

    private String format(ExperimentOpsHeaders headers, String message) {
        validateHeaders(headers);

        StringBuilder header = new StringBuilder();
        header.append("| requestUuid : ").append(headers.getRequestUuid());
        header.append(" | userUuid : ").append(headers.getUserUuid());
        header.append(" | workspaceUuid : ").append(headers.getWorkspaceUuid());

        if (StringUtils.isNotBlank(headers.getInternalTraceUuid())) {
            header.append(" | internalTraceUuid : ").append(headers.getInternalTraceUuid());
        }

        return header + " | " + message;
    }

    private void validateHeaders(ExperimentOpsHeaders headers) {
        if (StringUtils.isBlank(headers.getRequestUuid())) {
            headers.setRequestUuid(NA);
        }
        if (StringUtils.isBlank(headers.getUserUuid())) {
            headers.setUserUuid(NA);
        }
        if (StringUtils.isBlank(headers.getWorkspaceUuid())) {
            headers.setWorkspaceUuid(NA);
        }
    }
}
