package com.experimentops.platformapi.interfaces.listener;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.platformapi.service.WorkspaceService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.event.WorkspaceMutationEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceConsumer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(WorkspaceConsumer.class);
    public static final String CONSUMING_MESSAGE = "Consuming message: ";

    private final WorkspaceService workspaceService;

    @KafkaListener(topics = "${workspace.topic.name}", groupId = "${workspace.topic.group-id}")
    public void consume(ConsumerRecord<String, WorkspaceMutationEvent> workspaceRecord) {
        WorkspaceMutationEvent event = workspaceRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        EventType eventType = EventType.valueOf(event.getMetadata().getEventType());
        if (EventType.WORKSPACE_CREATE == eventType) {
            workspaceService.createWorkspace(event, headers);
        } else if (EventType.WORKSPACE_CREATION_COMPLETE == eventType) {
            workspaceService.changeStatusWorkspace(headers);
        }
    }

}
