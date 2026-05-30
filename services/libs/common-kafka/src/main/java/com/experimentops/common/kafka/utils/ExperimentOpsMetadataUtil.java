package com.experimentops.common.kafka.utils;

import com.experimentops.utils.HeaderUtil;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public final class ExperimentOpsMetadataUtil {

    private static final String NA = "N/A";

    private ExperimentOpsMetadataUtil() {}

    public static ExperimentOpsMetadataEvent metadataEvent(ExperimentOpsHeaders headers,
                                                           String uuid,
                                                           String eventType,
                                                           String className) {
        return metadataEvent(headers, uuid, eventType, UUID.randomUUID().toString(), className);
    }

    public static ExperimentOpsMetadataEvent metadataEvent(ExperimentOpsHeaders headers,
                                                           String uuid,
                                                           String eventType,
                                                           String eventUuid,
                                                           String className) {
        long requestTimestamp = parseRequestTimestamp(headers.getRequestTimestamp());

        return ExperimentOpsMetadataEvent.newBuilder()
                .setTraceUuid(headers.getRequestUuid())
                .setRequesterUuid(headers.getUserUuid())
                .setEventUuid(eventUuid)
                .setEventType(eventType)
                .setEventTimestamp(System.currentTimeMillis())
                .setUuid(uuid)
                .setWorkspaceUuid(headers.getWorkspaceUuid())
                .setRequestTimestamp(requestTimestamp)
                .setClassName(className)
                .setInternalTraceUuid(headers.getInternalTraceUuid())
                .setUserRole(headers.getUserRole())
                .build();
    }


    public static ExperimentOpsHeaders extractHeaders(ExperimentOpsMetadataEvent event) {
        ExperimentOpsHeaders headers = HeaderUtil.createHeadersForLogs(
                event.getTraceUuid(),
                event.getRequesterUuid(),
                event.getWorkspaceUuid()
        );
        headers.setRequestTimestamp(String.valueOf(event.getRequestTimestamp()));
        headers.setUserRole(StringUtils.defaultIfBlank(event.getUserRole(), NA));
        headers.setInternalTraceUuid(StringUtils.defaultIfBlank(event.getInternalTraceUuid(), NA));
        return headers;
    }


    private static long parseRequestTimestamp(String requestTimestamp) {
        if (StringUtils.isBlank(requestTimestamp)) {
            return System.currentTimeMillis();
        }
        try {
            return Long.parseLong(requestTimestamp);
        } catch (NumberFormatException ignored) {
            return System.currentTimeMillis();
        }
    }
}