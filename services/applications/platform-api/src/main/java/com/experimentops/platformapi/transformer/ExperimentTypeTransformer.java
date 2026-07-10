package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.experiment.type.event.ExperimentTypeMutationEvent;
import com.experimentops.experiment.type.event.ExperimentTypeMutationEventPayload;
import com.experimentops.experiment.type.model.v1.ExperimentTypeDefaultConfigModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeFormatMappingModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeRequestModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeResponseModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeStatusChangeRequestModel;
import com.experimentops.platformapi.model.entity.ExperimentType;
import com.experimentops.platformapi.model.entity.ExperimentTypeDefaultConfig;
import com.experimentops.platformapi.model.entity.ExperimentTypeFormatMapping;
import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.utils.JSONUtil;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ExperimentTypeTransformer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentTypeTransformer.class);

    @NonNull
    public ExperimentTypeMutationEvent transformExperimentTypeCreationEvent(@NonNull ExperimentTypeRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "transforming the payload to experiment type mutation event");

        ExperimentTypeMutationEventPayload payload = ExperimentTypeMutationEventPayload.newBuilder()
                .setName(normalizeExperimentTypeName(requestModel.getName()))
                .setDefaultConfig(writeDefaultConfig(requestModel.getDefaultConfig()))
                .setFormatMappings(writeFormatMappings(requestModel.getFormatMappings()))
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                ExperimentOpsUtils.uuid(),
                EventType.EXPERIMENT_TYPE_CREATE.name(),
                this.getClass().getSimpleName()
        );

        return ExperimentTypeMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    @NonNull
    public ExperimentTypeMutationEvent transformExperimentTypeUpdateEvent(@NonNull String uuid, @NonNull ExperimentTypeRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "transforming the payload to experiment type mutation event for update");

        ExperimentTypeMutationEventPayload payload = ExperimentTypeMutationEventPayload.newBuilder()
                .setName(normalizeExperimentTypeName(requestModel.getName()))
                .setDefaultConfig(writeDefaultConfig(requestModel.getDefaultConfig()))
                .setFormatMappings(writeFormatMappings(requestModel.getFormatMappings()))
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                uuid,
                EventType.EXPERIMENT_TYPE_UPDATE.name(),
                this.getClass().getSimpleName()
        );

        return ExperimentTypeMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    @NonNull
    public ExperimentTypeMutationEvent transformExperimentTypeStatusChangeEvent(@NonNull String uuid, @NonNull ExperimentTypeStatusChangeRequestModel statusChangeRequestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "transforming the payload to experiment type mutation event for status change");

        ExperimentTypeMutationEventPayload payload = ExperimentTypeMutationEventPayload.newBuilder()
                .setStatus(statusChangeRequestModel.getStatus())
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                uuid,
                EventType.EXPERIMENT_TYPE_STATUS_CHANGE.name(),
                this.getClass().getSimpleName()
        );

        return ExperimentTypeMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    @NonNull
    public ExperimentTypeResponseModel transformExperimentTypeResponseModel(@NonNull ExperimentTypeMutationEvent event, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "transforming the payload to Experiment Type Response Model");

        ExperimentTypeMutationEventPayload payload = event.getPayload();
        ExperimentTypeResponseModel responseModel = new ExperimentTypeResponseModel();
        responseModel.setUuid(event.getMetadata().getUuid());
        responseModel.setName(payload.getName());
        responseModel.setDefaultConfig(toDefaultConfigModel(toDefaultConfigEntity(payload.getDefaultConfig())));
        responseModel.setFormatMappings(toFormatMappingModel(toFormatMappingEntity(payload.getFormatMappings())));

        return responseModel;
    }

    @NonNull
    public ExperimentTypeResponseModel transformExperimentTypeResponseModelFromEntity(@NonNull ExperimentType experimentType, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "transforming the Experiment Type entity to Experiment Type Response Model");

        ExperimentTypeResponseModel responseModel = new ExperimentTypeResponseModel();
        responseModel.setUuid(experimentType.getUuid());
        responseModel.setName(experimentType.getName());
        responseModel.setDefaultConfig(toDefaultConfigModel(experimentType.getDefaultConfig()));
        responseModel.setFormatMappings(toFormatMappingModel(experimentType.getFormatMappings()));
        responseModel.setStatus(experimentType.getStatus().name());
        responseModel.setUpdatedAt(experimentType.getLastUpdated().toLocalDateTime().toString());

        return responseModel;
    }

    @NonNull
    public ExperimentType transformExperimentTypeEntity(@NonNull ExperimentTypeMutationEvent event, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "transforming the payload to Experiment Type Entity");

        ExperimentTypeMutationEventPayload payload = event.getPayload();
        ExperimentType experimentType = ExperimentType.builder()
                .name(payload.getName())
                .defaultConfig(toDefaultConfigEntity(payload.getDefaultConfig()))
                .formatMappings(toFormatMappingEntity(payload.getFormatMappings()))
                .status(StatusEnum.ACTIVE)
                .build();
        experimentType.setUuid(event.getMetadata().getUuid());

        return experimentType;
    }

    @NonNull
    public String normalizeExperimentTypeName(@NonNull String name) {
        return name.trim()
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase(Locale.ROOT);
    }

    private String writeDefaultConfig(List<ExperimentTypeDefaultConfigModel> defaultConfig) {
        List<ExperimentTypeDefaultConfig> configItems = defaultConfig == null
                ? Collections.emptyList()
                : defaultConfig.stream().map(this::toDefaultConfigItem).toList();
        String json = JSONUtil.toNonTypedJsonFromObject(configItems);
        if (StringUtils.isBlank(json)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "defaultConfig");
        }
        return json;
    }

    @NonNull
    public List<ExperimentTypeDefaultConfig> toDefaultConfigEntity(String defaultConfig) {
        if (StringUtils.isBlank(defaultConfig)) {
            return Collections.emptyList();
        }
        List<ExperimentTypeDefaultConfig> config = JSONUtil.toListFromTypedJson(defaultConfig, ExperimentTypeDefaultConfig.class);
        if (config.isEmpty()) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "defaultConfig");
        }
        return config;
    }

    @NonNull
    private List<ExperimentTypeDefaultConfigModel> toDefaultConfigModel(List<ExperimentTypeDefaultConfig> defaultConfig) {
        if (defaultConfig == null || defaultConfig.isEmpty()) {
            return Collections.emptyList();
        }
        return defaultConfig.stream().map(this::toDefaultConfigModel).toList();
    }

    private ExperimentTypeDefaultConfig toDefaultConfigItem(ExperimentTypeDefaultConfigModel configModel) {
        Object defaultValue = configModel.getDefaultValue() != null && configModel.getDefaultValue().isPresent()
                ? configModel.getDefaultValue().get()
                : null;
        return ExperimentTypeDefaultConfig.builder()
                .name(configModel.getName())
                .datatype(ExperimentTypeDefaultConfig.Datatype.fromValue(configModel.getDatatype().getValue()))
                .defaultValue(defaultValue)
                .regex(getNullableString(configModel.getRegex()))
                .build();
    }

    private ExperimentTypeDefaultConfigModel toDefaultConfigModel(ExperimentTypeDefaultConfig configItem) {
        return new ExperimentTypeDefaultConfigModel()
                .name(configItem.getName())
                .datatype(ExperimentTypeDefaultConfigModel.DatatypeEnum.fromValue(configItem.getDatatype().getValue()))
                .defaultValue(configItem.getDefaultValue())
                .regex(configItem.getRegex());
    }

    private String writeFormatMappings(List<ExperimentTypeFormatMappingModel> formatMappings) {
        List<ExperimentTypeFormatMapping> mappingItems = formatMappings == null
                ? Collections.emptyList()
                : formatMappings.stream().map(this::toFormatMappingItem).toList();
        String json = JSONUtil.toNonTypedJsonFromObject(mappingItems);
        if (StringUtils.isBlank(json)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings");
        }
        return json;
    }

    @NonNull
    public List<ExperimentTypeFormatMapping> toFormatMappingEntity(String formatMappings) {
        if (StringUtils.isBlank(formatMappings)) {
            return Collections.emptyList();
        }
        List<ExperimentTypeFormatMapping> mappings = JSONUtil.toListFromTypedJson(formatMappings, ExperimentTypeFormatMapping.class);
        if (mappings.isEmpty()) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings");
        }
        return mappings;
    }

    @NonNull
    private List<ExperimentTypeFormatMappingModel> toFormatMappingModel(List<ExperimentTypeFormatMapping> formatMappings) {
        if (formatMappings == null || formatMappings.isEmpty()) {
            return Collections.emptyList();
        }
        return formatMappings.stream().map(this::toFormatMappingModel).toList();
    }

    private ExperimentTypeFormatMapping toFormatMappingItem(ExperimentTypeFormatMappingModel formatMappingModel) {
        return ExperimentTypeFormatMapping.builder()
                .inputFormat(DatasetFileFormatEnum.valueOf(formatMappingModel.getInputFormat().getValue()))
                .outputFormat(DatasetFileFormatEnum.valueOf(formatMappingModel.getOutputFormat().getValue()))
                .build();
    }

    private ExperimentTypeFormatMappingModel toFormatMappingModel(ExperimentTypeFormatMapping formatMapping) {
        return new ExperimentTypeFormatMappingModel()
                .inputFormat(ExperimentTypeFormatMappingModel.InputFormatEnum.fromValue(formatMapping.getInputFormat().name()))
                .outputFormat(ExperimentTypeFormatMappingModel.OutputFormatEnum.fromValue(formatMapping.getOutputFormat().name()));
    }

    private String getNullableString(JsonNullable<String> value) {
        if (value == null || !value.isPresent()) {
            return null;
        }
        return value.get();
    }

}
