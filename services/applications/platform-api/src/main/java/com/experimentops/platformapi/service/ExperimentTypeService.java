package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityAlreadyExistsException;
import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.experiment.type.event.ExperimentTypeMutationEvent;
import com.experimentops.experiment.type.model.v1.ExperimentTypeListResponseModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeRequestModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeResponseModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeStatusChangeRequestModel;
import com.experimentops.platformapi.dal.repository.ExperimentTypeRepository;
import com.experimentops.platformapi.model.entity.ExperimentType;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.ExperimentTypeTransformer;
import com.experimentops.platformapi.validator.ExperimentTypeValidator;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperimentTypeService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentTypeService.class);

    private final ExperimentTypeValidator experimentTypeValidator;
    private final ExperimentTypeRepository experimentTypeRepository;
    private final ExperimentTypeTransformer experimentTypeTransformer;
    private final KafkaProducer kafkaProducer;

    private static final String EXPERIMENT_TYPE_UUID = "experiment_type_uuid";

    @Value("${experiment.type.topic.name}")
    private String experimentTypeTopic;

    @NonNull
    public ExperimentTypeResponseModel publishExperimentTypeCreationEvent(@NonNull ExperimentTypeRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        experimentTypeValidator.validateExperimentTypeRequestModel(requestModel);
        assert requestModel.getName() != null;
        String experimentTypeName = experimentTypeTransformer.normalizeExperimentTypeName(requestModel.getName());
        log.info(headers, "Creating new experiment type with name " + experimentTypeName);
        experimentTypeRepository
                .findByNameAndStatusAndEnabled(experimentTypeName, StatusEnum.ACTIVE, true)
                .ifPresent(experimentType -> {
                    throw new EntityAlreadyExistsException("name", experimentTypeName);
                });
        ExperimentTypeMutationEvent event = experimentTypeTransformer.transformExperimentTypeCreationEvent(requestModel, headers);
        kafkaProducer.sendMessage(experimentTypeTopic, event, event.getMetadata());
        return experimentTypeTransformer.transformExperimentTypeResponseModel(event, headers);
    }

    public void createExperimentType(@NonNull ExperimentTypeMutationEvent event, @NonNull ExperimentOpsHeaders headers) {
        ExperimentType experimentType = experimentTypeTransformer.transformExperimentTypeEntity(event, headers);
        experimentTypeRepository.save(experimentType);
        log.info(headers, "saved experiment type with uuid: " + experimentType.getUuid());
    }

    @NonNull
    public ExperimentTypeResponseModel publishExperimentTypeUpdateEvent(@NonNull String uuid, @NonNull ExperimentTypeRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "updating experiment type with uuid " + uuid);
        experimentTypeValidator.validateExperimentTypeRequestModel(requestModel);
        assert requestModel.getName() != null;
        String experimentTypeName = experimentTypeTransformer.normalizeExperimentTypeName(requestModel.getName());
        experimentTypeRepository
                .findByNameAndStatusAndEnabled(experimentTypeName, StatusEnum.ACTIVE, true)
                .filter(experimentType -> !uuid.equals(experimentType.getUuid()))
                .ifPresent(experimentType -> {
                    throw new EntityAlreadyExistsException("name", experimentTypeName);
                });
        experimentTypeRepository
                .findByUuidAndStatusAndEnabled(uuid, StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException(EXPERIMENT_TYPE_UUID, uuid));
        ExperimentTypeMutationEvent event = experimentTypeTransformer.transformExperimentTypeUpdateEvent(uuid, requestModel, headers);
        kafkaProducer.sendMessage(experimentTypeTopic, event, event.getMetadata());
        return experimentTypeTransformer.transformExperimentTypeResponseModel(event, headers);
    }

    public void updateExperimentType(@NonNull ExperimentTypeMutationEvent event, @NonNull ExperimentOpsHeaders headers) {
        String experimentTypeUuid = event.getMetadata().getUuid();
        experimentTypeRepository
                .findByUuidAndStatusAndEnabled(experimentTypeUuid, StatusEnum.ACTIVE, true)
                .ifPresentOrElse(
                        experimentType -> {
                            experimentType.setName(event.getPayload().getName());
                            experimentType.setDefaultConfig(experimentTypeTransformer.toDefaultConfigEntity(event.getPayload().getDefaultConfig()));
                            experimentTypeRepository.save(experimentType);
                        },
                        () -> {
                            throw new EntityNotFoundException(EXPERIMENT_TYPE_UUID, experimentTypeUuid);
                        }
                );
        log.info(headers, "experiment type update completed");
    }

    @NonNull
    public ExperimentTypeResponseModel publishExperimentTypeStatusChangeEvent(@NonNull String uuid, @NonNull ExperimentTypeStatusChangeRequestModel statusChangeRequestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "changing status of experiment type with uuid " + uuid);
        ExperimentType experimentType = experimentTypeRepository
                .findByUuidAndEnabled(uuid, true)
                .orElseThrow(() -> new EntityNotFoundException(EXPERIMENT_TYPE_UUID, uuid));
        ExperimentTypeMutationEvent event = experimentTypeTransformer.transformExperimentTypeStatusChangeEvent(uuid, statusChangeRequestModel, headers);
        kafkaProducer.sendMessage(experimentTypeTopic, event, event.getMetadata());
        return experimentTypeTransformer.transformExperimentTypeResponseModelFromEntity(experimentType, headers);
    }

    public void changeStatusExperimentType(@NonNull ExperimentTypeMutationEvent event, @NonNull ExperimentOpsHeaders headers) {
        String experimentTypeUuid = event.getMetadata().getUuid();
        StatusEnum newStatus = StatusEnum.valueOf(event.getPayload().getStatus());
        experimentTypeRepository
                .findByUuidAndEnabled(experimentTypeUuid, true)
                .ifPresentOrElse(
                        experimentType -> {
                            experimentType.setStatus(newStatus);
                            experimentTypeRepository.save(experimentType);
                        },
                        () -> {
                            throw new EntityNotFoundException(EXPERIMENT_TYPE_UUID, experimentTypeUuid);
                        }
                );
        log.info(headers, "experiment type status change completed");
    }

    @NonNull
    public ExperimentTypeListResponseModel getExperimentTypeList(@NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting experiment type list");
        List<ExperimentType> experimentTypesResult = experimentTypeRepository
                .findAllByStatusAndEnabledOrderByLastUpdatedDesc(StatusEnum.ACTIVE, true);
        List<ExperimentTypeResponseModel> experimentTypes = experimentTypesResult
                .stream()
                .map(experimentType -> experimentTypeTransformer.transformExperimentTypeResponseModelFromEntity(experimentType, headers))
                .toList();
        ExperimentTypeListResponseModel response = new ExperimentTypeListResponseModel();
        response.setData(experimentTypes);
        response.setTotalElements((long) experimentTypes.size());
        return response;
    }

}
