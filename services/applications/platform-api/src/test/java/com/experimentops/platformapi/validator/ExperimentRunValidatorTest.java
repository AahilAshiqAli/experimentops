package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.experiment.run.model.v1.ExperimentRunExecutionModeModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunInputModel;
import com.experimentops.platformapi.model.ExperimentRunConfigContext;
import com.experimentops.platformapi.model.ExperimentRunResolvedPlan;
import com.experimentops.platformapi.model.entity.DatasetVersion;
import com.experimentops.platformapi.model.entity.DownStreamPolicyEnum;
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
import com.experimentops.platformapi.model.type.DatasetScanStatusEnum;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExperimentRunValidatorTest {
    private static final String DATASET_VERSION_UUID = "dataset-version-1";
    private static final String FIRST_CONFIG_UUID = "config-1";
    private static final String SECOND_CONFIG_UUID = "config-2";

    private final ExperimentRunValidator validator = new ExperimentRunValidator();

    @Test
    void marksOptionalOutputRequiredForRunWhenDownstreamStepReferencesIt() {
        ExperimentRunResolvedPlan plan = validator.validateAndResolveExecutionPlan(
                List.of(
                        step(1, FIRST_CONFIG_UUID, datasetInput("dataset", DATASET_VERSION_UUID)),
                        step(2, SECOND_CONFIG_UUID, artifactInput("cleanedData", 1, "cleanedData"))
                ),
                Map.of(
                        FIRST_CONFIG_UUID, config(FIRST_CONFIG_UUID, manifest(
                                List.of(input("dataset", true, DatasetFileFormatEnum.CSV)),
                                List.of(outputSameAsInput(
                                        "cleanedData",
                                        false,
                                        OutputDataKindEnum.TABULAR_DATASET,
                                        "dataset",
                                        DownStreamPolicyEnum.CONNECTABLE
                                ))
                        )),
                        SECOND_CONFIG_UUID, config(SECOND_CONFIG_UUID, manifest(
                                List.of(input("cleanedData", true, DatasetFileFormatEnum.CSV)),
                                List.of(outputFixed(
                                        "report",
                                        true,
                                        OutputDataKindEnum.REPORT,
                                        DatasetFileFormatEnum.JSON,
                                        DownStreamPolicyEnum.TERMINAL
                                ))
                        ))
                ),
                Map.of(DATASET_VERSION_UUID, datasetVersion(DATASET_VERSION_UUID, "CSV", DatasetScanStatusEnum.COMPLETED))
        );

        assertThat(plan.getSteps().getFirst().getOutputs().getFirst().getRequiredForRun()).isTrue();
    }

    @Test
    void keepsUnreferencedOptionalOutputOptionalForRun() {
        ExperimentRunResolvedPlan plan = validator.validateAndResolveExecutionPlan(
                List.of(step(1, FIRST_CONFIG_UUID, datasetInput("dataset", DATASET_VERSION_UUID))),
                Map.of(FIRST_CONFIG_UUID, config(FIRST_CONFIG_UUID, manifest(
                        List.of(input("dataset", true, DatasetFileFormatEnum.CSV)),
                        List.of(outputSameAsInput(
                                "debugData",
                                false,
                                OutputDataKindEnum.TABULAR_DATASET,
                                "dataset",
                                DownStreamPolicyEnum.CONNECTABLE
                        ))
                ))),
                Map.of(DATASET_VERSION_UUID, datasetVersion(DATASET_VERSION_UUID, "CSV", DatasetScanStatusEnum.COMPLETED))
        );

        assertThat(plan.getSteps().getFirst().getOutputs().getFirst().getRequiredForRun()).isFalse();
    }

    @Test
    void rejectsDatasetVersionThatHasNotCompletedScan() {
        assertThatThrownBy(() -> validator.validateAndResolveExecutionPlan(
                List.of(step(1, FIRST_CONFIG_UUID, datasetInput("dataset", DATASET_VERSION_UUID))),
                Map.of(FIRST_CONFIG_UUID, config(FIRST_CONFIG_UUID, manifest(
                        List.of(input("dataset", true, DatasetFileFormatEnum.CSV)),
                        List.of(outputSameAsInput(
                                "cleanedData",
                                true,
                                OutputDataKindEnum.TABULAR_DATASET,
                                "dataset",
                                DownStreamPolicyEnum.CONNECTABLE
                        ))
                ))),
                Map.of(DATASET_VERSION_UUID, datasetVersion(DATASET_VERSION_UUID, "CSV", DatasetScanStatusEnum.IN_PROGRESS))
        )).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsDatasetVersionWithUnknownFormatString() {
        assertThatThrownBy(() -> validator.validateAndResolveExecutionPlan(
                List.of(step(1, FIRST_CONFIG_UUID, datasetInput("dataset", DATASET_VERSION_UUID))),
                Map.of(FIRST_CONFIG_UUID, config(FIRST_CONFIG_UUID, manifest(
                        List.of(input("dataset", true, DatasetFileFormatEnum.CSV)),
                        List.of(outputSameAsInput(
                                "cleanedData",
                                true,
                                OutputDataKindEnum.TABULAR_DATASET,
                                "dataset",
                                DownStreamPolicyEnum.CONNECTABLE
                        ))
                ))),
                Map.of(DATASET_VERSION_UUID, datasetVersion(DATASET_VERSION_UUID, "BAD", DatasetScanStatusEnum.COMPLETED))
        )).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsTerminalOrInternalArtifactsAsDownstreamInputs() {
        assertThatThrownBy(() -> validator.validateAndResolveExecutionPlan(
                List.of(
                        step(1, FIRST_CONFIG_UUID, datasetInput("dataset", DATASET_VERSION_UUID)),
                        step(2, SECOND_CONFIG_UUID, artifactInput("terminalData", 1, "terminalData"))
                ),
                Map.of(
                        FIRST_CONFIG_UUID, config(FIRST_CONFIG_UUID, manifest(
                                List.of(input("dataset", true, DatasetFileFormatEnum.CSV)),
                                List.of(outputSameAsInput(
                                        "terminalData",
                                        true,
                                        OutputDataKindEnum.TABULAR_DATASET,
                                        "dataset",
                                        DownStreamPolicyEnum.TERMINAL
                                ))
                        )),
                        SECOND_CONFIG_UUID, config(SECOND_CONFIG_UUID, manifest(
                                List.of(input("terminalData", true, DatasetFileFormatEnum.CSV)),
                                List.of(outputFixed(
                                        "report",
                                        true,
                                        OutputDataKindEnum.REPORT,
                                        DatasetFileFormatEnum.JSON,
                                        DownStreamPolicyEnum.TERMINAL
                                ))
                        ))
                ),
                Map.of(DATASET_VERSION_UUID, datasetVersion(DATASET_VERSION_UUID, "CSV", DatasetScanStatusEnum.COMPLETED))
        )).isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> validator.validateAndResolveExecutionPlan(
                List.of(
                        step(1, FIRST_CONFIG_UUID, datasetInput("dataset", DATASET_VERSION_UUID)),
                        step(2, SECOND_CONFIG_UUID, artifactInput("internalData", 1, "internalData"))
                ),
                Map.of(
                        FIRST_CONFIG_UUID, config(FIRST_CONFIG_UUID, manifest(
                                List.of(input("dataset", true, DatasetFileFormatEnum.CSV)),
                                List.of(outputSameAsInput(
                                        "internalData",
                                        true,
                                        OutputDataKindEnum.TABULAR_DATASET,
                                        "dataset",
                                        DownStreamPolicyEnum.INTERNAL
                                ))
                        )),
                        SECOND_CONFIG_UUID, config(SECOND_CONFIG_UUID, manifest(
                                List.of(input("internalData", true, DatasetFileFormatEnum.CSV)),
                                List.of(outputFixed(
                                        "report",
                                        true,
                                        OutputDataKindEnum.REPORT,
                                        DatasetFileFormatEnum.JSON,
                                        DownStreamPolicyEnum.TERMINAL
                                ))
                        ))
                ),
                Map.of(DATASET_VERSION_UUID, datasetVersion(DATASET_VERSION_UUID, "CSV", DatasetScanStatusEnum.COMPLETED))
        )).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsSameFormatRelationshipMismatch() {
        assertThatThrownBy(() -> validator.validateAndResolveExecutionPlan(
                List.of(step(
                        1,
                        FIRST_CONFIG_UUID,
                        datasetInput("trainData", "train-version"),
                        datasetInput("testData", "test-version")
                )),
                Map.of(FIRST_CONFIG_UUID, config(FIRST_CONFIG_UUID, manifest(
                        List.of(
                                input("trainData", true, DatasetFileFormatEnum.CSV, DatasetFileFormatEnum.JSON),
                                input("testData", true, DatasetFileFormatEnum.CSV, DatasetFileFormatEnum.JSON)
                        ),
                        List.of(relationship("trainData", "testData")),
                        List.of(outputFixed(
                                "report",
                                true,
                                OutputDataKindEnum.REPORT,
                                DatasetFileFormatEnum.JSON,
                                DownStreamPolicyEnum.TERMINAL
                        ))
                ))),
                Map.of(
                        "train-version", datasetVersion("train-version", "CSV", DatasetScanStatusEnum.COMPLETED),
                        "test-version", datasetVersion("test-version", "JSON", DatasetScanStatusEnum.COMPLETED)
                )
        )).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsArtifactInputWithoutEarlierSourceStep() {
        assertThatThrownBy(() -> validator.validateExperimentRunRequestModel(
                "experiment-1",
                new com.experimentops.experiment.run.model.v1.ExperimentRunRequestModel()
                        .executionMode(List.of(step(1, FIRST_CONFIG_UUID, artifactInput("data", 1, "data")))),
                false
        )).isInstanceOf(ValidationException.class);
    }

    private static ExperimentRunConfigContext config(String uuid, ExperimentTypeManifest manifest) {
        return ExperimentRunConfigContext.builder()
                .experimentConfigUuid(uuid)
                .experimentType("CSV_PROFILE_ANALYSIS")
                .formatMappings(List.of(manifest))
                .timeWeight(1)
                .build();
    }

    private static ExperimentTypeManifest manifest(List<InputManifest> inputs, List<OutputManifest> outputs) {
        return manifest(inputs, List.of(), outputs);
    }

    private static ExperimentTypeManifest manifest(
            List<InputManifest> inputs,
            List<InputRelationship> relationships,
            List<OutputManifest> outputs) {

        return ExperimentTypeManifest.builder()
                .inputs(inputs)
                .inputRelationships(relationships)
                .outputs(outputs)
                .build();
    }

    private static InputManifest input(String portName, boolean required, DatasetFileFormatEnum... acceptedFormats) {
        return InputManifest.builder()
                .portName(portName)
                .required(required)
                .cardinality("ONE")
                .contract(InputContract.builder()
                        .dataKind(OutputDataKindEnum.TABULAR_DATASET.name())
                        .acceptedFormats(List.of(acceptedFormats))
                        .build())
                .build();
    }

    private static InputRelationship relationship(String... ports) {
        return InputRelationship.builder()
                .type(InputRelationshipTypeEnum.SAME_FORMAT)
                .ports(List.of(ports))
                .build();
    }

    private static OutputManifest outputSameAsInput(
            String name,
            boolean required,
            OutputDataKindEnum dataKind,
            String sourceInputPort,
            DownStreamPolicyEnum downStreamPolicy) {

        return OutputManifest.builder()
                .name(name)
                .required(required)
                .dataKind(dataKind)
                .type(FormatStrategy.builder()
                        .type(FormatStrategyTypeEnum.SAME_AS_INPUT)
                        .sourceInputPort(sourceInputPort)
                        .build())
                .downStreamPolicy(downStreamPolicy)
                .build();
    }

    private static OutputManifest outputFixed(
            String name,
            boolean required,
            OutputDataKindEnum dataKind,
            DatasetFileFormatEnum format,
            DownStreamPolicyEnum downStreamPolicy) {

        return OutputManifest.builder()
                .name(name)
                .required(required)
                .dataKind(dataKind)
                .type(FormatStrategy.builder()
                        .type(FormatStrategyTypeEnum.FIXED)
                        .format(format)
                        .build())
                .downStreamPolicy(downStreamPolicy)
                .build();
    }

    private static ExperimentRunExecutionModeModel step(
            int stepCount,
            String experimentConfigUuid,
            ExperimentRunInputModel... inputs) {

        return new ExperimentRunExecutionModeModel()
                .stepCount(stepCount)
                .experimentConfigUuid(experimentConfigUuid)
                .inputs(List.of(inputs));
    }

    private static ExperimentRunInputModel datasetInput(String portName, String datasetVersionUuid) {
        return new ExperimentRunInputModel()
                .portName(portName)
                .inputType(ExperimentRunInputModel.InputTypeEnum.DATASET)
                .file(datasetVersionUuid);
    }

    private static ExperimentRunInputModel artifactInput(String portName, int sourceStepCount, String artifactName) {
        return new ExperimentRunInputModel()
                .portName(portName)
                .inputType(ExperimentRunInputModel.InputTypeEnum.ARTIFACT)
                .file(artifactName)
                .sourceStepCount(sourceStepCount);
    }

    private static DatasetVersion datasetVersion(String uuid, String format, DatasetScanStatusEnum scanStatus) {
        DatasetVersion datasetVersion = DatasetVersion.builder()
                .format(format)
                .scanStatus(scanStatus)
                .status(StatusEnum.ACTIVE)
                .storageUri("s3://bucket/" + uuid)
                .build();
        datasetVersion.setUuid(uuid);
        return datasetVersion;
    }
}
