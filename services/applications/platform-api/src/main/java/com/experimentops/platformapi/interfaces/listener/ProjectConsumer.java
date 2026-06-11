package com.experimentops.platformapi.interfaces.listener;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.platformapi.service.ProjectService;
import com.experimentops.project.event.ProjectMutationEvent;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectConsumer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ProjectConsumer.class);
    public static final String CONSUMING_MESSAGE = "Consuming message: ";

    private final ProjectService projectService;

    @KafkaListener(topics = "${project.topic.name}", groupId = "${project.topic.group-id}")
    public void consume(ConsumerRecord<String, ProjectMutationEvent> projectRecord) {
        ProjectMutationEvent event = projectRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        EventType eventType = EventType.valueOf(event.getMetadata().getEventType());
        if (EventType.PROJECT_CREATE == eventType) {
            projectService.createProject(event, headers);
        } else if (EventType.PROJECT_UPDATE == eventType) {
            projectService.updateProject(event, headers);
        } else if (EventType.PROJECT_STATUS_CHANGE == eventType) {
            projectService.changeStatusProject(event, headers);
        }
    }

}
