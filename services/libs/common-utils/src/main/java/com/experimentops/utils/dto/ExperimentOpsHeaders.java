package com.experimentops.utils.dto;

import lombok.Data;

@Data
public class ExperimentOpsHeaders {
    String requestUuid;
    String workspaceUuid;
    String userUuid;
    String userName;
    String userRole;
    String authorizationToken;
    String requestTimestamp;
    String userAgent;
    String clientIp;
    String internalTraceUuid;
    long requestCounter = -1L;

    // Returns new headers with set request uuid.
    public static ExperimentOpsHeaders requestUuid(String requestUuid) {
        ExperimentOpsHeaders headers = new ExperimentOpsHeaders();
        headers.setRequestUuid(requestUuid);
        return headers;
    }

    public ExperimentOpsHeaders copy() {
        ExperimentOpsHeaders headers = new ExperimentOpsHeaders();
        headers.requestUuid = this.requestUuid;
        headers.workspaceUuid = this.workspaceUuid;
        headers.userUuid = this.userUuid;
        headers.userName = this.userName;
        headers.userRole = this.userRole;
        headers.authorizationToken = this.authorizationToken;
        headers.requestTimestamp = this.requestTimestamp;
        headers.userAgent = this.userAgent;
        headers.clientIp = this.clientIp;
        headers.internalTraceUuid = this.internalTraceUuid;
        headers.requestCounter = this.requestCounter;
        return headers;
    }
}