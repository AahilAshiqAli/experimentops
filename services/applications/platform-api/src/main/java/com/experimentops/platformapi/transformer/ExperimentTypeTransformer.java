package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.experiment.type.event.ExperimentTypeMutationEvent;
import com.experimentops.experiment.type.event.ExperimentTypeMutationEventPayload;
import com.experimentops.experiment.type.model.v1.ExperimentTypeDefaultConfigModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeManifestModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeRequestModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeResponseModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeStatusChangeRequestModel;
import com.experimentops.experiment.type.model.v1.FormatStrategyModel;
import com.experimentops.experiment.type.model.v1.InputContractModel;
import com.experimentops.experiment.type.model.v1.InputManifestModel;
import com.experimentops.experiment.type.model.v1.InputRelationshipModel;
import com.experimentops.experiment.type.model.v1.OutputManifestModel;
import com.experimentops.platformapi.model.entity.DownStreamPolicyEnum;
import com.experimentops.platformapi.model.entity.ExperimentType;
import com.experimentops.platformapi.model.entity.ExperimentTypeDefaultConfig;
import com.experimentops.platformapi.model.entity.ExperimentTypeManifest;
import com.experimentops.platformapi.model.entity.FormatStrategy;
import com.experimentops.platformapi.model.entity.FormatStrategyTypeEnum;
import com.experimentops.platformapi.model.entity.InputContract;
import com.experimentops.platformapi.model.entity.InputManifest;
import com.experimentops.platformapi.model.entity.InputRelationship;
import com.experimentops.platformapi.model.entity.InputRelationshipTypeEnum;
import com.experimentops.platformapi.model.entity.OutputDataKindEnum;
import com.experimentops.platformapi.model.entity.OutputManifest;
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
                .setTimeWeight(requestModel.getTimeWeight())
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
                .setTimeWeight(requestModel.getTimeWeight())
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
        responseModel.setTimeWeight(payload.getTimeWeight());

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
        responseModel.setTimeWeight(experimentType.getTimeWeight());
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
                .timeWeight(payload.getTimeWeight())
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

    private String writeFormatMappings(List<ExperimentTypeManifestModel> formatMappings) {
        List<ExperimentTypeManifest> mappingItems = formatMappings == null
                ? Collections.emptyList()
                : formatMappings.stream().map(this::toManifestEntity).toList();
        String json = JSONUtil.toNonTypedJsonFromObject(mappingItems);
        if (StringUtils.isBlank(json)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings");
        }
        return json;
    }

    @NonNull
    public List<ExperimentTypeManifest> toFormatMappingEntity(String formatMappings) {
        if (StringUtils.isBlank(formatMappings)) {
            return Collections.emptyList();
        }
        List<ExperimentTypeManifest> mappings = JSONUtil.toListFromTypedJson(formatMappings, ExperimentTypeManifest.class);
        if (mappings.isEmpty()) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings");
        }
        return mappings;
    }

    @NonNull
    private List<ExperimentTypeManifestModel> toFormatMappingModel(List<ExperimentTypeManifest> formatMappings) {
        if (formatMappings == null || formatMappings.isEmpty()) {
            return Collections.emptyList();
        }
        return formatMappings.stream().map(this::toManifestModel).toList();
    }

    private ExperimentTypeManifest toManifestEntity(ExperimentTypeManifestModel manifestModel) {
        return ExperimentTypeManifest.builder()
                .inputs(toInputManifestEntity(manifestModel.getInputs()))
                .inputRelationships(toInputRelationshipEntity(manifestModel.getInputRelationships()))
                .outputs(toOutputManifestEntity(manifestModel.getOutputs()))
                .build();
    }

    private ExperimentTypeManifestModel toManifestModel(ExperimentTypeManifest manifest) {
        return new ExperimentTypeManifestModel()
                .inputs(toInputManifestModel(manifest.getInputs()))
                .inputRelationships(toInputRelationshipModel(manifest.getInputRelationships()))
                .outputs(toOutputManifestModel(manifest.getOutputs()));
    }

    private List<InputManifest> toInputManifestEntity(List<InputManifestModel> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return Collections.emptyList();
        }
        return inputs.stream().map(this::toInputManifestEntity).toList();
    }

    private InputManifest toInputManifestEntity(InputManifestModel input) {
        return InputManifest.builder()
                .portName(input.getPortName())
                .required(input.getRequired())
                .cardinality(input.getCardinality())
                .contract(toInputContractEntity(input.getContract()))
                .build();
    }

    private InputContract toInputContractEntity(InputContractModel contract) {
        if (contract == null) {
            return null;
        }
        return InputContract.builder()
                .dataKind(contract.getDataKind().getValue())
                .acceptedFormats(contract.getAcceptedFormats().stream()
                        .map(format -> DatasetFileFormatEnum.valueOf(format.getValue()))
                        .toList())
                .build();
    }

    private List<InputManifestModel> toInputManifestModel(List<InputManifest> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return Collections.emptyList();
        }
        return inputs.stream().map(this::toInputManifestModel).toList();
    }

    private InputManifestModel toInputManifestModel(InputManifest input) {
        return new InputManifestModel()
                .portName(input.getPortName())
                .required(input.getRequired())
                .cardinality(input.getCardinality())
                .contract(toInputContractModel(input.getContract()));
    }

    private InputContractModel toInputContractModel(InputContract contract) {
        if (contract == null) {
            return null;
        }
        return new InputContractModel()
                .dataKind(InputContractModel.DataKindEnum.fromValue(contract.getDataKind()))
                .acceptedFormats(contract.getAcceptedFormats().stream()
                        .map(format -> InputContractModel.AcceptedFormatsEnum.fromValue(format.name()))
                        .toList());
    }

    private List<InputRelationship> toInputRelationshipEntity(List<InputRelationshipModel> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return Collections.emptyList();
        }
        return relationships.stream().map(this::toInputRelationshipEntity).toList();
    }

    private InputRelationship toInputRelationshipEntity(InputRelationshipModel relationship) {
        return InputRelationship.builder()
                .type(InputRelationshipTypeEnum.valueOf(relationship.getType().getValue()))
                .ports(relationship.getPorts())
                .build();
    }

    private List<InputRelationshipModel> toInputRelationshipModel(List<InputRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return Collections.emptyList();
        }
        return relationships.stream().map(this::toInputRelationshipModel).toList();
    }

    private InputRelationshipModel toInputRelationshipModel(InputRelationship relationship) {
        return new InputRelationshipModel()
                .type(InputRelationshipModel.TypeEnum.fromValue(relationship.getType().name()))
                .ports(relationship.getPorts());
    }

    private List<OutputManifest> toOutputManifestEntity(List<OutputManifestModel> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return Collections.emptyList();
        }
        return outputs.stream().map(this::toOutputManifestEntity).toList();
    }

    private OutputManifest toOutputManifestEntity(OutputManifestModel output) {
        return OutputManifest.builder()
                .name(output.getName())
                .required(output.getRequired())
                .dataKind(OutputDataKindEnum.valueOf(output.getDataKind().getValue()))
                .type(toFormatStrategyEntity(output.getType()))
                .downStreamPolicy(DownStreamPolicyEnum.valueOf(output.getDownStreamPolicy().getValue()))
                .build();
    }

    private FormatStrategy toFormatStrategyEntity(FormatStrategyModel strategy) {
        if (strategy == null) {
            return null;
        }
        FormatStrategy.FormatStrategyBuilder builder = FormatStrategy.builder()
                .type(FormatStrategyTypeEnum.valueOf(strategy.getType().getValue()))
                .sourceInputPort(getNullableString(strategy.getSourceInputPort()));
        FormatStrategyModel.FormatEnum format = getNullableFormat(strategy.getFormat());
        if (format != null) {
            builder.format(DatasetFileFormatEnum.valueOf(format.getValue()));
        }
        return builder.build();
    }

    private List<OutputManifestModel> toOutputManifestModel(List<OutputManifest> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return Collections.emptyList();
        }
        return outputs.stream().map(this::toOutputManifestModel).toList();
    }

    private OutputManifestModel toOutputManifestModel(OutputManifest output) {
        return new OutputManifestModel()
                .name(output.getName())
                .required(output.getRequired())
                .dataKind(OutputManifestModel.DataKindEnum.fromValue(output.getDataKind().name()))
                .type(toFormatStrategyModel(output.getType()))
                .downStreamPolicy(OutputManifestModel.DownStreamPolicyEnum.fromValue(output.getDownStreamPolicy().name()));
    }

    private FormatStrategyModel toFormatStrategyModel(FormatStrategy strategy) {
        if (strategy == null) {
            return null;
        }
        FormatStrategyModel model = new FormatStrategyModel()
                .type(FormatStrategyModel.TypeEnum.fromValue(strategy.getType().name()));
        if (StringUtils.isNotBlank(strategy.getSourceInputPort())) {
            model.sourceInputPort(strategy.getSourceInputPort());
        }
        if (strategy.getFormat() != null) {
            model.format(FormatStrategyModel.FormatEnum.fromValue(strategy.getFormat().name()));
        }
        return model;
    }

    private String getNullableString(JsonNullable<String> value) {
        if (value == null || !value.isPresent()) {
            return null;
        }
        return value.get();
    }

    private FormatStrategyModel.FormatEnum getNullableFormat(JsonNullable<FormatStrategyModel.FormatEnum> value) {
        if (value == null || !value.isPresent()) {
            return null;
        }
        return value.get();
    }

}
