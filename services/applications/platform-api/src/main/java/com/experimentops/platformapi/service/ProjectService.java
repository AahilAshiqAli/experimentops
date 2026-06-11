package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityAlreadyExistsException;
import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.platformapi.dal.repository.ProjectRepository;
import com.experimentops.platformapi.model.entity.Project;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.ProjectTransformer;
import com.experimentops.platformapi.validator.ProjectValidator;
import com.experimentops.project.event.ProjectMutationEvent;
import com.experimentops.project.model.v1.ProjectRequestModel;
import com.experimentops.project.model.v1.ProjectResponseModel;
import com.experimentops.project.model.v1.ProjectStatusChangeRequestModel;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ProjectService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ProjectService.class);

    private final ProjectValidator projectValidator;
    private final ProjectRepository projectRepository;
    private final ProjectTransformer projectTransformer;
    private final KafkaProducer kafkaProducer;

    @Value("${project.topic.name}")
    private String projectTopic;

    @NonNull
    public ProjectResponseModel publishProjectCreationEvent(@NonNull ProjectRequestModel projectRequestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "Creating new project with name " + projectRequestModel.getName());
        projectValidator.validateProjectRequestModel(projectRequestModel);
        projectRepository
                .findByNameAndWorkspaceUuidAndStatusAndEnabled(
                        projectRequestModel.getName(),
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE,
                        true)
                .ifPresent(project -> {
                    throw new EntityAlreadyExistsException("name", projectRequestModel.getName());
                });
        ProjectMutationEvent projectMutationEvent = projectTransformer.transformProjectCreationEvent(projectRequestModel, headers);
        kafkaProducer.sendMessage(projectTopic, projectMutationEvent, projectMutationEvent.getMetadata());
        return projectTransformer.transformProjectResponseModel(projectMutationEvent, headers);
    }

    public void createProject(@NonNull ProjectMutationEvent projectMutationEvent, @NonNull ExperimentOpsHeaders headers) {
        Project project = projectTransformer.transformProjectEntity(projectMutationEvent, headers);
        projectRepository.save(project);
        log.info(headers, "saved project with project uuid: " + project.getUuid());
    }

    @NonNull
    public ProjectResponseModel publishProjectUpdateEvent(@NonNull String uuid, @NonNull ProjectRequestModel projectRequestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "updating project with uuid " + uuid);
        projectValidator.validateProjectRequestModel(projectRequestModel);
        projectRepository
                .findByUuidAndWorkspaceUuidAndStatusAndEnabled(uuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException("project_uuid", uuid));
        ProjectMutationEvent projectMutationEvent = projectTransformer.transformProjectUpdateEvent(uuid, projectRequestModel, headers);
        kafkaProducer.sendMessage(projectTopic, projectMutationEvent, projectMutationEvent.getMetadata());
        return projectTransformer.transformProjectResponseModel(projectMutationEvent, headers);
    }

    public void updateProject(@NonNull ProjectMutationEvent projectMutationEvent, @NonNull ExperimentOpsHeaders headers) {
        String projectUuid = projectMutationEvent.getMetadata().getUuid();
        projectRepository
                .findByUuidAndWorkspaceUuidAndStatusAndEnabled(projectUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE,true)
                .ifPresentOrElse(
                        project -> {
                            project.setName(projectMutationEvent.getPayload().getProjectName());
                            project.setDescription(projectMutationEvent.getPayload().getDescription());
                            projectRepository.save(project);
                        },
                        () -> {
                            throw new EntityNotFoundException("Project uuid", projectUuid);
                        }
                );
        log.info(headers, "project update completed");
    }

    @NonNull
    public ProjectResponseModel publishProjectStatusChangeEvent(@NonNull String uuid, @NonNull ProjectStatusChangeRequestModel statusChangeRequestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "changing status of project with uuid " + uuid);
        Project project = projectRepository
                .findByUuidAndWorkspaceUuidAndEnabled(uuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException("Project uuid", uuid));
        ProjectMutationEvent projectMutationEvent = projectTransformer.transformProjectStatusChangeEvent(uuid, statusChangeRequestModel, headers);
        kafkaProducer.sendMessage(projectTopic, projectMutationEvent, projectMutationEvent.getMetadata());
        return projectTransformer.transformProjectResponseModelFromEntity(project, headers);
    }

    public void changeStatusProject(@NonNull ProjectMutationEvent projectMutationEvent, @NonNull ExperimentOpsHeaders headers) {
        String projectUuid = projectMutationEvent.getMetadata().getUuid();
        StatusEnum newStatus = StatusEnum.valueOf(projectMutationEvent.getPayload().getStatus());
        projectRepository
                .findByUuidAndWorkspaceUuidAndEnabled(projectUuid, headers.getWorkspaceUuid(), true)
                .ifPresentOrElse(
                        project -> {
                            project.setStatus(newStatus);
                            projectRepository.save(project);
                        },
                        () -> {
                            throw new EntityNotFoundException("Project uuid", projectUuid);
                        }
                );
        log.info(headers, "project status change completed");
    }

    @NonNull
    public ProjectResponseModel getProject(@NonNull String uuid, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting project with uuid " + uuid);
        Project project = projectRepository
                .findByUuidAndWorkspaceUuidAndStatusAndEnabled(uuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException("Project uuid", uuid));
        return projectTransformer.transformProjectResponseModelFromEntity(project, headers);
    }

    @NonNull
    public List<ProjectResponseModel> getProjectList(@NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting project list for workspace uuid " + headers.getWorkspaceUuid());
        return projectRepository
                .findAllByWorkspaceUuidAndStatusAndEnabled(headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .stream()
                .map(project -> projectTransformer.transformProjectResponseModelFromEntity(project, headers))
                .toList();
    }

}
