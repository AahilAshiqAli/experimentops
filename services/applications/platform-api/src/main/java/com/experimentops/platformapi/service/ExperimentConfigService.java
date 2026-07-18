package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityAlreadyExistsException;
import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.experiment.model.v1.*;
import com.experimentops.platformapi.dal.repository.ExperimentConfigRepository;
import com.experimentops.platformapi.dal.repository.ExperimentConfigWithTypeProjection;
import com.experimentops.platformapi.dal.repository.ExperimentTypeRepository;
import com.experimentops.platformapi.model.entity.ExperimentConfig;
import com.experimentops.platformapi.model.entity.ExperimentType;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.ExperimentConfigTransformer;
import com.experimentops.platformapi.transformer.ExperimentTypeTransformer;
import com.experimentops.platformapi.validator.ExperimentConfigValidator;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperimentConfigService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentConfigService.class);
    private static final String EXPERIMENT_UUID = "experiment_uuid";
    private static final String EXPERIMENT_CONFIG_UUID = "experiment_config_uuid";

    private final ExperimentConfigValidator experimentConfigValidator;
    private final ExperimentConfigRepository experimentConfigRepository;
    private final ExperimentConfigTransformer experimentConfigTransformer;
    private final ExperimentTypeTransformer experimentTypeTransformer;
    private final ExperimentTypeRepository experimentTypeRepository;

    @NonNull
    public ExperimentConfigResponseModel createExperimentConfig(@NonNull String experimentUuid, @NonNull ExperimentConfigRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "creating experiment config with name " + requestModel.getName());
        experimentConfigValidator.validateExperimentConfigRequestModel(requestModel);
        ExperimentType experimentType = findActiveExperimentType(requestModel.getExperimentType());
        experimentConfigValidator.validateExperimentConfigMatchesExperimentTypeConfig(experimentType, requestModel.getConfig());
        experimentConfigRepository
                .findByNameAndExperimentUuidAndWorkspaceUuidAndStatusAndEnabled(
                        requestModel.getName(),
                        experimentUuid,
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE,
                        true)
                .ifPresent(experimentConfig -> {
                    throw new EntityAlreadyExistsException("name", requestModel.getName());
                });

        ExperimentConfig experimentConfig = experimentConfigTransformer.transformExperimentConfigEntity(experimentUuid, requestModel, headers);
        experimentConfig = experimentConfigRepository.save(experimentConfig);
        return experimentConfigTransformer.transformExperimentConfigResponseModelFromEntity(experimentConfig, headers);
    }

    @NonNull
    public ExperimentConfigResponse getExperimentConfig(@NonNull String experimentUuid, @NonNull String uuid, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting experiment config with uuid " + uuid);
        List<String> experimentConfigUuid = new ArrayList<>(List.of(uuid));

        List<ExperimentConfigWithTypeProjection> experimentConfig = experimentConfigRepository.findAllWithExperimentTypesByUuidIn(experimentConfigUuid,
                experimentUuid, headers.getWorkspaceUuid(),  StatusEnum.ACTIVE.getCode());
        return experimentConfigTransformer.transformExperimentConfigResponseFromEntity(experimentConfig, headers);
    }

    @NonNull
    public ExperimentConfigListResponseModel getExperimentConfigList(@NonNull String experimentUuid, Integer page, Integer size, String experimentType, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting experiment config list for experiment uuid " + experimentUuid);
        Pageable pageable = PaginationUtil.createPageRequest(page, size);
        Page<ExperimentConfig> experimentConfigPage = StringUtils.isBlank(experimentType)
                ? experimentConfigRepository.findAllByExperimentUuidAndWorkspaceUuidAndStatusAndEnabledOrderByLastUpdatedDesc(
                        experimentUuid,
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE,
                        true,
                        pageable)
                : experimentConfigRepository.findAllByExperimentUuidAndWorkspaceUuidAndExperimentTypeAndStatusAndEnabledOrderByLastUpdatedDesc(
                        experimentUuid,
                        headers.getWorkspaceUuid(),
                        experimentTypeTransformer.normalizeExperimentTypeName(experimentType),
                        StatusEnum.ACTIVE,
                        true,
                        pageable);
        List<ExperimentConfigResponseModel> experimentConfigs = experimentConfigPage
                .getContent()
                .stream()
                .map(experimentConfig -> experimentConfigTransformer.transformExperimentConfigResponseModelFromEntity(experimentConfig, headers))
                .toList();

        ExperimentConfigListResponseModel response = new ExperimentConfigListResponseModel();
        response.setData(experimentConfigs);
        response.setTotalElements(experimentConfigPage.getTotalElements());
        return response;
    }

    @NonNull
    public ExperimentConfigResponseModel updateExperimentConfig(@NonNull String experimentUuid, @NonNull String uuid, @NonNull ExperimentConfigRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "updating experiment config with uuid " + uuid);
        experimentConfigValidator.validateExperimentConfigRequestModel(requestModel);
        ExperimentType experimentType = findActiveExperimentType(requestModel.getExperimentType());
        experimentConfigValidator.validateExperimentConfigMatchesExperimentTypeConfig(experimentType, requestModel.getConfig());
        ExperimentConfig experimentConfig = findActiveExperimentConfig(experimentUuid, uuid, headers);
        experimentConfigRepository
                .findByNameAndExperimentUuidAndWorkspaceUuidAndStatusAndEnabled(
                        requestModel.getName(),
                        experimentUuid,
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE,
                        true)
                .filter(existingExperimentConfig -> !existingExperimentConfig.getUuid().equals(uuid))
                .ifPresent(existingExperimentConfig -> {
                    throw new EntityAlreadyExistsException("name", requestModel.getName());
                });

        experimentConfigTransformer.updateExperimentConfigEntity(experimentConfig, requestModel, headers);
        experimentConfig = experimentConfigRepository.save(experimentConfig);
        return experimentConfigTransformer.transformExperimentConfigResponseModelFromEntity(experimentConfig, headers);
    }

    @NonNull
    public ExperimentConfigResponseModel updateStatusExperimentConfig(@NonNull String experimentUuid, @NonNull String uuid, @NonNull ExperimentConfigStatusChangeRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "changing status of experiment config with uuid " + uuid);
        StatusEnum status = experimentConfigValidator.validateExperimentConfigStatusChangeRequestModel(requestModel);
        ExperimentConfig experimentConfig = experimentConfigRepository
                .findByUuidAndExperimentUuidAndWorkspaceUuidAndEnabled(uuid, experimentUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException(EXPERIMENT_CONFIG_UUID, uuid));
        experimentConfig.setStatus(status);
        experimentConfig.setUpdatedBy(headers.getUserUuid());
        experimentConfig = experimentConfigRepository.save(experimentConfig);
        return experimentConfigTransformer.transformExperimentConfigResponseModelFromEntity(experimentConfig, headers);
    }

    @NonNull
    private ExperimentType findActiveExperimentType(@NonNull String experimentType) {
        return experimentTypeRepository
                .findByNameAndStatusAndEnabled(experimentType, StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException("experiment_type not found for " + experimentType));
    }

    @NonNull
    private ExperimentConfig findActiveExperimentConfig(@NonNull String experimentUuid, @NonNull String uuid, @NonNull ExperimentOpsHeaders headers) {
        return experimentConfigRepository
                .findByUuidAndExperimentUuidAndWorkspaceUuidAndStatusAndEnabled(
                        uuid,
                        experimentUuid,
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE,
                        true)
                .orElseThrow(() -> new EntityNotFoundException(EXPERIMENT_CONFIG_UUID, uuid));
    }
}
