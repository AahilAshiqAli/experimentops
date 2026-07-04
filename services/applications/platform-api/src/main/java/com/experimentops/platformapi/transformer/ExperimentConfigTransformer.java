package com.experimentops.platformapi.transformer;

import com.experimentops.experiment.model.v1.ExperimentConfigRequestModel;
import com.experimentops.experiment.model.v1.ExperimentConfigResponseModel;
import com.experimentops.platformapi.model.entity.ExperimentConfig;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.JSONUtil;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

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
        responseModel.setConfig(toConfigMap(experimentConfig));
        responseModel.setExperimentUuid(experimentConfig.getExperimentUuid());
        responseModel.setStatus(experimentConfig.getStatus().name());
        responseModel.setUpdatedAt(experimentConfig.getLastUpdated().toLocalDateTime().toString());
        return responseModel;
    }

    public void updateExperimentConfigEntity(
            @NonNull ExperimentConfig experimentConfig,
            @NonNull ExperimentConfigRequestModel requestModel,
            @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "updating ExperimentConfig entity fields");

        experimentConfig.setName(requestModel.getName());
        experimentConfig.setConfig(toConfigJsonNode(requestModel.getConfig()));
        experimentConfig.setUpdatedBy(headers.getUserUuid());
    }

    @NonNull
    private Map<String, Object> toConfigMap(@NonNull ExperimentConfig experimentConfig) {
        if (experimentConfig.getConfig() == null || experimentConfig.getConfig().isNull()) {
            return Map.of();
        }
        return JSONUtil.toMapFromJson(JSONUtil.toNonTypedJsonFromObject(experimentConfig.getConfig()));
    }

    @NonNull
    private JsonNode toConfigJsonNode(@NonNull Map<String, Object> config) {
        return JSONUtil.toObjectFromTypedJson(JSONUtil.toNonTypedJsonFromObject(config), JsonNode.class);
    }
}
