package com.experimentops.platformapi.transformer;

import com.experimentops.experiment.model.v1.ExperimentConfigRequestModel;
import com.experimentops.experiment.model.v1.ExperimentConfigResponse;
import com.experimentops.experiment.model.v1.ExperimentConfigResponseModel;
import com.experimentops.experiment.model.v1.ExperimentTypeManifestModel;
import com.experimentops.platformapi.dal.repository.ExperimentConfigWithTypeProjection;
import com.experimentops.platformapi.model.entity.ExperimentConfig;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.JSONUtil;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ExperimentConfigTransformer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentConfigTransformer.class);

    @NonNull
    public ExperimentConfig transformExperimentConfigEntity(
            @NonNull String experimentUuid,
            @NonNull ExperimentConfigRequestModel requestModel,
            @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "transforming the payload to ExperimentConfig Entity");

        return ExperimentConfig.builder()
                .name(requestModel.getName())
                .experimentType(requestModel.getExperimentType())
                .config(toConfigJsonNode(requestModel.getConfig()))
                .experimentUuid(experimentUuid)
                .workspaceUuid(headers.getWorkspaceUuid())
                .status(StatusEnum.ACTIVE)
                .build();
    }

    @NonNull
    public ExperimentConfigResponseModel transformExperimentConfigResponseModelFromEntity(
            @NonNull ExperimentConfig experimentConfig,
            @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "transforming the ExperimentConfig entity to ExperimentConfig Response Model");

        ExperimentConfigResponseModel responseModel = new ExperimentConfigResponseModel();
        responseModel.setUuid(experimentConfig.getUuid());
        responseModel.setName(experimentConfig.getName());
        responseModel.setExperimentType(experimentConfig.getExperimentType());
        responseModel.setConfig(JSONUtil.toMapFromJson(JSONUtil.toNonTypedJsonFromObject(experimentConfig.getConfig())));
        responseModel.setExperimentUuid(experimentConfig.getExperimentUuid());
        responseModel.setStatus(experimentConfig.getStatus().name());
        responseModel.setUpdatedAt(experimentConfig.getLastUpdated().toLocalDateTime().toString());
        return responseModel;
    }


    public ExperimentConfigResponse transformExperimentConfigResponseFromEntity(
            @NonNull List<ExperimentConfigWithTypeProjection> experimentConfigs,
            @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "transforming the ExperimentConfig entity to ExperimentConfig Response Model");

        ExperimentConfigResponse responseModel = new ExperimentConfigResponse();

        if (ExperimentOpsUtils.isEmpty(experimentConfigs)){
            return responseModel;
        }
        ExperimentConfigWithTypeProjection experimentConfig = experimentConfigs.getFirst();
        responseModel.setUuid(experimentConfig.getExperimentConfigUuid());
        responseModel.setName(experimentConfig.getName());
        responseModel.setExperimentType(experimentConfig.getExperimentType());
        responseModel.setConfig(toConfigMap(experimentConfig.getConfig()));
        responseModel.setFormatMappings(toFormatMappings(experimentConfig.getFormatMappings()));
        return responseModel;
    }

    public void updateExperimentConfigEntity(
            @NonNull ExperimentConfig experimentConfig,
            @NonNull ExperimentConfigRequestModel requestModel,
            @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "updating ExperimentConfig entity fields");

        experimentConfig.setName(requestModel.getName());
        experimentConfig.setExperimentType(requestModel.getExperimentType());
        experimentConfig.setConfig(toConfigJsonNode(requestModel.getConfig()));
        experimentConfig.setUpdatedBy(headers.getUserUuid());
    }

    @NonNull
    private JsonNode toConfigJsonNode(@NonNull Map<String, Object> config) {
        return JSONUtil.toObjectFromTypedJson(JSONUtil.toNonTypedJsonFromObject(config), JsonNode.class);
    }

    @NonNull
    private Map<String, Object> toConfigMap(String config) {
        if (StringUtils.isBlank(config)) {
            return Map.of();
        }
        return JSONUtil.toMapFromJson(config);
    }

    @NonNull
    private List<ExperimentTypeManifestModel> toFormatMappings(String formatMappings) {
        if (StringUtils.isBlank(formatMappings)) {
            return List.of();
        }
        return JSONUtil.toListFromTypedJson(formatMappings, ExperimentTypeManifestModel.class);
    }
}
