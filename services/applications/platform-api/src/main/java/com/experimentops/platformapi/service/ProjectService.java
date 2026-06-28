package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityAlreadyExistsException;
import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.platformapi.dal.repository.*;
import com.experimentops.platformapi.model.entity.*;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.ProjectTransformer;
import com.experimentops.platformapi.validator.ProjectValidator;
import com.experimentops.project.event.ProjectMutationEvent;
import com.experimentops.project.model.v1.ProjectListResponseModel;
import com.experimentops.project.model.v1.ProjectRequestModel;
import com.experimentops.project.model.v1.ProjectResponseModel;
import com.experimentops.project.model.v1.ProjectSummaryResponseModel;
import com.experimentops.project.model.v1.ProjectStatusChangeRequestModel;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ProjectService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ProjectService.class);

    private final ProjectValidator projectValidator;
    private final ProjectRepository projectRepository;
    private final ProjectSummaryRepository projectSummaryRepository;
    private final ProjectTransformer projectTransformer;
    private final KafkaProducer kafkaProducer;
    private final ExperimentRepository experimentRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository datasetVersionRepository;

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
    public ProjectSummaryResponseModel getProject(@NonNull String uuid, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting project with uuid " + uuid);
        ProjectSummary projectSummary = projectSummaryRepository
                .findByProjectUuidAndWorkspaceUuidAndStatusAndEnabled(uuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException("Project uuid", uuid));

        return projectTransformer.transformProjectSummaryResponseModel(projectSummary, headers);
    }

    @NonNull
    public ProjectListResponseModel getProjectList(Integer page, Integer size, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting project list for workspace uuid " + headers.getWorkspaceUuid());
        Pageable pageable = PaginationUtil.createPageRequest(page, size);
        Page<Project> projectsPage = projectRepository
                .findAllByWorkspaceUuidAndStatusAndEnabledOrderByCreationDateDesc(headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true, pageable);
        List<ProjectResponseModel> projects = projectsPage
                .getContent()
                .stream()
                .map(projectTransformer::transformProjectResponseModel)
                .toList();
        ProjectListResponseModel response = new ProjectListResponseModel();
        response.setData(projects);
        response.setTotalElements(projectsPage.getTotalElements());
        return response;
    }

}
