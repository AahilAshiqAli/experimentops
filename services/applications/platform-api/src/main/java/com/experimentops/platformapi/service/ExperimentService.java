package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityAlreadyExistsException;
import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.experiment.event.ExperimentMutationEvent;
import com.experimentops.experiment.model.v1.ExperimentListItemModel;
import com.experimentops.experiment.model.v1.ExperimentRequestModel;
import com.experimentops.experiment.model.v1.ExperimentResponseModel;
import com.experimentops.experiment.model.v1.ExperimentStatusChangeRequestModel;
import com.experimentops.platformapi.dal.repository.ExperimentConfigRepository;
import com.experimentops.platformapi.dal.repository.ExperimentRepository;
import com.experimentops.platformapi.dal.repository.ExperimentRunRepository;
import com.experimentops.platformapi.model.entity.Experiment;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.ExperimentTransformer;
import com.experimentops.platformapi.validator.ExperimentValidator;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ExperimentService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentService.class);

    private final ExperimentValidator experimentValidator;
    private final ExperimentRepository experimentRepository;
    private final ExperimentConfigRepository experimentConfigRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final ExperimentTransformer experimentTransformer;
    private final KafkaProducer kafkaProducer;

    @Value("${experiment.topic.name}")
    private String experimentTopic;

    private static final String EXPERIMENT_UUID = "experiment_uuid";

    @NonNull
    public ExperimentResponseModel publishExperimentCreationEvent(@NonNull String projectUuid, @NonNull ExperimentRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "Creating new experiment with name " + requestModel.getName());
        experimentValidator.validateExperimentRequestModel(requestModel);
        experimentRepository
                .findByNameAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(
                        requestModel.getName(),
                        projectUuid,
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE,
                        true)
                .ifPresent(experiment -> {
                    throw new EntityAlreadyExistsException("name", requestModel.getName());
                });
        ExperimentMutationEvent experimentMutationEvent = experimentTransformer.transformExperimentCreationEvent(projectUuid, requestModel, headers);
        kafkaProducer.sendMessage(experimentTopic, experimentMutationEvent, experimentMutationEvent.getMetadata());
        return experimentTransformer.transformExperimentResponseModel(experimentMutationEvent, headers);
    }

    public void createExperiment(@NonNull ExperimentMutationEvent experimentMutationEvent, @NonNull ExperimentOpsHeaders headers) {
        Experiment experiment = experimentTransformer.transformExperimentEntity(experimentMutationEvent, headers);
        experimentRepository.save(experiment);
        log.info(headers, "saved experiment with experiment uuid: " + experiment.getUuid());
    }

    @NonNull
    public ExperimentResponseModel publishExperimentUpdateEvent(@NonNull String uuid, @NonNull String projectUuid, @NonNull ExperimentRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "updating experiment with uuid " + uuid);
        experimentValidator.validateExperimentRequestModel(requestModel);
        experimentRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(uuid, projectUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException(EXPERIMENT_UUID, uuid));
        ExperimentMutationEvent experimentMutationEvent = experimentTransformer.transformExperimentUpdateEvent(uuid, projectUuid, requestModel, headers);
        kafkaProducer.sendMessage(experimentTopic, experimentMutationEvent, experimentMutationEvent.getMetadata());
        return experimentTransformer.transformExperimentResponseModel(experimentMutationEvent, headers);
    }

    public void updateExperiment(@NonNull ExperimentMutationEvent experimentMutationEvent, @NonNull ExperimentOpsHeaders headers) {
        String experimentUuid = experimentMutationEvent.getMetadata().getUuid();
        String projectUuid = experimentMutationEvent.getPayload().getProjectUuid();
        experimentRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(experimentUuid, projectUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .ifPresentOrElse(
                        experiment -> {
                            experiment.setName(experimentMutationEvent.getPayload().getExperimentName());
                            experiment.setDescription(experimentMutationEvent.getPayload().getDescription());
                            experiment.setExperimentType(experimentMutationEvent.getPayload().getExperimentType());
                            experimentRepository.save(experiment);
                        },
                        () -> {
                            throw new EntityNotFoundException(EXPERIMENT_UUID, experimentUuid);
                        }
                );
        log.info(headers, "experiment update completed");
    }

    @NonNull
    public ExperimentResponseModel publishExperimentStatusChangeEvent(@NonNull String uuid, @NonNull String projectUuid, @NonNull ExperimentStatusChangeRequestModel statusChangeRequestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "changing status of experiment with uuid " + uuid);
        Experiment experiment = experimentRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndEnabled(uuid, projectUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException(EXPERIMENT_UUID, uuid));
        ExperimentMutationEvent experimentMutationEvent = experimentTransformer.transformExperimentStatusChangeEvent(uuid, projectUuid, statusChangeRequestModel, headers);
        kafkaProducer.sendMessage(experimentTopic, experimentMutationEvent, experimentMutationEvent.getMetadata());
        return experimentTransformer.transformExperimentResponseModelFromEntity(experiment, headers);
    }

    public void changeStatusExperiment(@NonNull ExperimentMutationEvent experimentMutationEvent, @NonNull ExperimentOpsHeaders headers) {
        String experimentUuid = experimentMutationEvent.getMetadata().getUuid();
        String projectUuid = experimentMutationEvent.getPayload().getProjectUuid();
        StatusEnum newStatus = StatusEnum.valueOf(experimentMutationEvent.getPayload().getStatus());
        experimentRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndEnabled(experimentUuid, projectUuid, headers.getWorkspaceUuid(), true)
                .ifPresentOrElse(
                        experiment -> {
                            experiment.setStatus(newStatus);
                            experimentRepository.save(experiment);
                        },
                        () -> {
                            throw new EntityNotFoundException(EXPERIMENT_UUID, experimentUuid);
                        }
                );
        log.info(headers, "experiment status change completed");
    }

    // Todo: Correct the response format
    @NonNull
    public ExperimentResponseModel getExperiment(@NonNull String uuid, @NonNull String projectUuid, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting experiment with uuid " + uuid);
        Experiment experiment = experimentRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(uuid, projectUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException(EXPERIMENT_UUID, uuid));
        return experimentTransformer.transformExperimentResponseModelFromEntity(experiment, headers);
    }

    @NonNull
    public List<ExperimentListItemModel> getExperimentList(@NonNull String projectUuid, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting experiment list for project uuid " + projectUuid);
        return experimentRepository
                .findAllByProjectUuidAndWorkspaceUuidAndStatusAndEnabledOrderByCreationDateDesc(projectUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .stream()
                .map(experiment -> {
                    int configCount = (int) experimentConfigRepository.countByExperimentUuidAndEnabled(experiment.getUuid(), true);
                    int runCount = (int) experimentRunRepository.countByExperimentUuidAndEnabled(experiment.getUuid(), true);
                    return experimentTransformer.transformExperimentListItemModel(experiment, configCount, runCount, headers);
                })
                .toList();
    }

}
