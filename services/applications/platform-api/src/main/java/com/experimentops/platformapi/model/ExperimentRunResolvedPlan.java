package com.experimentops.platformapi.model;

import com.experimentops.platformapi.model.entity.DownStreamPolicyEnum;
import com.experimentops.platformapi.model.entity.FormatStrategyTypeEnum;
import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentRunResolvedPlan {
    private List<Step> steps;


    public List<DatasetAttachment> datasetAttachments() {
        if (steps == null) {
            return List.of();
        }
        return steps.stream()
                .filter(Objects::nonNull)
                .flatMap(step -> step.getInputs() == null
                        ? List.<DatasetAttachment>of().stream()
                        : step.getInputs().stream()
                        .filter(input -> input.getDatasetVersionUuid() != null)
                        .map(input -> new DatasetAttachment(
                                step.getStepCount(),
                                input.getPortName(),
                                input.getDatasetVersionUuid()
                        )))
                .toList();
    }

    public String firstDatasetUri() {
        if (steps == null) {
            return null;
        }
        return steps.stream()
                .filter(Objects::nonNull)
                .flatMap(step -> step.getInputs() == null ? List.<Input>of().stream() : step.getInputs().stream())
                .map(Input::getDatasetUri)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
        private Integer stepCount;
        private String experimentConfigUuid;
        private String experimentType;
        private JsonNode experimentConfigJson;
        private Integer timeWeight;
        private List<Input> inputs;
        private List<Output> outputs;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private String portName;
        private String inputType;
        private String dataKind;
        private DatasetFileFormatEnum format;
        private String datasetVersionUuid;
        private String datasetUri;
        private Integer sourceStepCount;
        private String artifactName;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        private String name;
        private String dataKind;
        private FormatStrategyTypeEnum formatStrategy;
        private DatasetFileFormatEnum format;
        private String sourceInputPort;
        private DownStreamPolicyEnum downStreamPolicy;
        private Boolean requiredForRun;
    }

    public record DatasetAttachment(
            Integer stepCount,
            String portName,
            String datasetVersionUuid
    ) {
    }
}
