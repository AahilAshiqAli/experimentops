package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.platformapi.model.entity.Project;
import com.experimentops.platformapi.model.entity.ProjectSummary;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.project.event.ProjectMutationEvent;
import com.experimentops.project.event.ProjectMutationEventPayload;
import com.experimentops.project.model.v1.ProjectRequestModel;
import com.experimentops.project.model.v1.ProjectResponseModel;
import com.experimentops.project.model.v1.ProjectSummaryResponseModel;
import com.experimentops.project.model.v1.ProjectStatusChangeRequestModel;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ProjectTransformer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ProjectTransformer.class);

    public ProjectMutationEvent transformProjectCreationEvent(@NonNull ProjectRequestModel projectRequestModel, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to project mutation event");

        ProjectMutationEventPayload payload = ProjectMutationEventPayload.newBuilder()
                .setProjectName(projectRequestModel.getName())
                .setDescription(projectRequestModel.getDescription())
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                ExperimentOpsUtils.uuid(),
                EventType.PROJECT_CREATE.name(),
                this.getClass().getSimpleName()
        );

        return ProjectMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public ProjectMutationEvent transformProjectUpdateEvent(@NonNull String uuid, @NonNull ProjectRequestModel projectRequestModel, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to project mutation event for update");

        ProjectMutationEventPayload payload = ProjectMutationEventPayload.newBuilder()
                .setProjectName(projectRequestModel.getName())
                .setDescription(projectRequestModel.getDescription())
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                uuid,
                EventType.PROJECT_UPDATE.name(),
                this.getClass().getSimpleName()
        );

        return ProjectMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public ProjectMutationEvent transformProjectStatusChangeEvent(@NonNull String uuid, @NonNull ProjectStatusChangeRequestModel statusChangeRequestModel, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to project mutation event for status change");

        ProjectMutationEventPayload payload = ProjectMutationEventPayload.newBuilder()
                .setStatus(statusChangeRequestModel.getStatus())
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                uuid,
                EventType.PROJECT_STATUS_CHANGE.name(),
                this.getClass().getSimpleName()
        );

        return ProjectMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public ProjectResponseModel transformProjectResponseModel(@NonNull ProjectMutationEvent projectMutationEvent, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to Project Response Model");

        ProjectMutationEventPayload payload = projectMutationEvent.getPayload();

        ProjectResponseModel projectResponseModel = new ProjectResponseModel();
        projectResponseModel.setUuid(projectMutationEvent.getMetadata().getUuid());
        projectResponseModel.setName(payload.getProjectName());
        projectResponseModel.setDescription(payload.getDescription());

        return projectResponseModel;
    }

    public ProjectResponseModel transformProjectResponseModel(@NonNull Project project) {


        ProjectResponseModel projectResponseModel = new ProjectResponseModel();
        projectResponseModel.setUuid(project.getUuid());
        projectResponseModel.setName(project.getName());
        projectResponseModel.setDescription(project.getDescription());
        projectResponseModel.setCreationDate(String.valueOf(project.getCreationDate()));

        return projectResponseModel;
    }

    @NonNull
    public ProjectResponseModel transformProjectResponseModelFromEntity(@NonNull Project project, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the Project entity to Project Response Model");

        ProjectResponseModel projectResponseModel = new ProjectResponseModel();
        projectResponseModel.setUuid(project.getUuid());
        projectResponseModel.setName(project.getName());
        projectResponseModel.setDescription(project.getDescription());

        return projectResponseModel;
    }

    @NonNull
    public ProjectSummaryResponseModel transformProjectSummaryResponseModel(@NonNull ProjectSummary projectSummary, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the ProjectSummary entity to Project Summary Response Model");

        ProjectSummaryResponseModel responseModel = new ProjectSummaryResponseModel();
        responseModel.setProjectUuid(projectSummary.getProjectUuid());
        responseModel.setName(projectSummary.getName());
        responseModel.setDescription(projectSummary.getDescription());
        responseModel.setCreatedAt(projectSummary.getCreatedAt().toLocalDateTime().toString());
        responseModel.setDatasetCount((int) projectSummary.getDatasetCount());
        responseModel.setExperimentCount((int) projectSummary.getExperimentCount());
        responseModel.setExperimentConfigCount((int) projectSummary.getExperimentConfigCount());
        responseModel.setDatasetVersionCount((int) projectSummary.getDatasetVersionCount());
        responseModel.setExperimentRunCount((int) projectSummary.getExperimentRunCount());

        return responseModel;
    }

    @NonNull
    public Project transformProjectEntity(@NonNull ProjectMutationEvent projectMutationEvent, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to Project Entity");

        ProjectMutationEventPayload payload = projectMutationEvent.getPayload();
        Project project = Project.builder()
                .name(payload.getProjectName())
                .description(payload.getDescription())
                .workspaceUuid(projectMutationEvent.getMetadata().getWorkspaceUuid())
                .status(StatusEnum.ACTIVE)
                .build();
        project.setUuid(projectMutationEvent.getMetadata().getUuid());

        return project;
    }

}
