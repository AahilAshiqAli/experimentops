package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.experiment.run.event.ExperimentRunCompletedArtifact;
import com.experimentops.experiment.run.event.ExperimentRunCompletedEvent;
import com.experimentops.experiment.run.event.ExperimentRunCompletedEventPayload;
import com.experimentops.experiment.run.event.ExperimentRunCompletedResult;
import com.experimentops.experiment.run.model.v1.ExperimentRunExecutionModeModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunCompareRequestModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunInputModel;
import com.experimentops.platformapi.model.ExperimentRunConfigContext;
import com.experimentops.platformapi.model.ExperimentRunResolvedPlan;
import com.experimentops.platformapi.model.entity.DatasetVersion;
import com.experimentops.platformapi.model.entity.DownStreamPolicyEnum;
import com.experimentops.platformapi.model.entity.ExecutionMode;
import com.experimentops.platformapi.model.entity.ExecutionModeInput;
import com.experimentops.platformapi.model.entity.ExperimentRun;
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
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
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

    @Test
    void rejectsCompletedEventWhenRequiredArtifactIsMissing() {
        ExperimentRunResolvedPlan plan = resolvedPlan(outputFixed(
                "report",
                true,
                OutputDataKindEnum.REPORT,
                DatasetFileFormatEnum.JSON,
                DownStreamPolicyEnum.TERMINAL
        ));

        assertThatThrownBy(() -> validator.validateCompletedArtifacts(
                completedEvent(List.of()),
                plan,
                Map.of(1, "CSV_PROFILE_ANALYSIS_NORMALIZE")
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("CSV_PROFILE_ANALYSIS_NORMALIZE failed step number : 1. Did not produce required artifact: report");
    }

    @Test
    void validatesProducedOptionalArtifact() {
        ExperimentRunResolvedPlan plan = resolvedPlan(outputFixed(
                "debugData",
                false,
                OutputDataKindEnum.TABULAR_DATASET,
                DatasetFileFormatEnum.CSV,
                DownStreamPolicyEnum.CONNECTABLE
        ));

        assertThatThrownBy(() -> validator.validateCompletedArtifacts(
                completedEvent(List.of(completedArtifact(1, "debugData", "TABULAR_DATASET", "JSON"))),
                plan,
                Map.of(1, "CSV_PROFILE_ANALYSIS_NORMALIZE")
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Required artifact debugData expected format CSV but got JSON");
    }

    @Test
    void rejectsCompletedEventWithUnexpectedArtifact() {
        ExperimentRunResolvedPlan plan = resolvedPlan(outputFixed(
                "report",
                true,
                OutputDataKindEnum.REPORT,
                DatasetFileFormatEnum.JSON,
                DownStreamPolicyEnum.TERMINAL
        ));

        assertThatThrownBy(() -> validator.validateCompletedArtifacts(
                completedEvent(List.of(
                        completedArtifact(1, "report", "REPORT", "JSON"),
                        completedArtifact(1, "extra", "REPORT", "JSON")
                )),
                plan,
                Map.of(1, "CSV_PROFILE_ANALYSIS_NORMALIZE")
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("CSV_PROFILE_ANALYSIS_NORMALIZE failed step number : 1. Produced unexpected artifact: extra");
    }

    @Test
    void acceptsConfigComparisonWhenOnlyConfigBindingsDiffer() {
        ExperimentRun first = comparableRun("run-1", "dataset-1", "evaluation-config-1");
        ExperimentRun second = comparableRun("run-2", "dataset-1", "evaluation-config-2");
        second.setExecutionMode(List.of(
                second.getExecutionMode().get(1),
                second.getExecutionMode().get(0)
        ));

        validator.validateExperimentRunsToCompare(
                List.of(first, second),
                compareRequest(ExperimentRunCompareRequestModel.ComparisonAxisEnum.CONFIG)
        );
    }

    @Test
    void acceptsDatasetComparisonWhenOnlyDatasetBindingsDiffer() {
        validator.validateExperimentRunsToCompare(
                List.of(
                        comparableRun("run-1", "dataset-1", "evaluation-config"),
                        comparableRun("run-2", "dataset-2", "evaluation-config")
                ),
                compareRequest(ExperimentRunCompareRequestModel.ComparisonAxisEnum.DATASET)
        );
    }

    @Test
    void rejectsComparisonWhenConfigsAndDatasetsBothDiffer() {
        assertThatThrownBy(() -> validator.validateExperimentRunsToCompare(
                List.of(
                        comparableRun("run-1", "dataset-1", "evaluation-config-1"),
                        comparableRun("run-2", "dataset-2", "evaluation-config-2")
                ),
                compareRequest(ExperimentRunCompareRequestModel.ComparisonAxisEnum.CONFIG)
        )).isInstanceOf(ValidationException.class)
                .hasMessage("Runs must differ only along the selected comparison axis");
    }

    @Test
    void rejectsCompareRequestWithDuplicateRunUuids() {
        ExperimentRunCompareRequestModel request = new ExperimentRunCompareRequestModel(
                List.of("run-1", "run-1"),
                ExperimentRunCompareRequestModel.ComparisonAxisEnum.CONFIG
        );

        assertThatThrownBy(() -> validator.validateExperimentRunCompareRequestModel(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsComparisonWhenPipelineSignaturesDiffer() {
        ExperimentRun first = comparableRun("run-1", "dataset-1", "evaluation-config-1");
        ExperimentRun second = comparableRun("run-2", "dataset-1", "evaluation-config-2");
        second.getExecutionMode().get(1).setExperimentType("REGRESSION_EVALUATION");

        assertThatThrownBy(() -> validator.validateExperimentRunsToCompare(
                List.of(first, second),
                compareRequest(ExperimentRunCompareRequestModel.ComparisonAxisEnum.CONFIG)
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

    private static ExperimentRunCompareRequestModel compareRequest(
            ExperimentRunCompareRequestModel.ComparisonAxisEnum comparisonAxis) {
        return new ExperimentRunCompareRequestModel(List.of("run-1", "run-2"), comparisonAxis);
    }

    private static ExperimentRun comparableRun(
            String runUuid,
            String datasetVersionUuid,
            String evaluationConfigUuid) {
        ExperimentRun run = ExperimentRun.builder()
                .experimentUuid("experiment-1")
                .experimentStatus(ExperimentStatusEnum.SUCCEEDED)
                .executionMode(List.of(
                        ExecutionMode.builder()
                                .stepCount(1)
                                .experimentType("TABULAR_TRAIN_TEST_SPLIT")
                                .experimentConfigUuid("split-config")
                                .inputs(List.of(ExecutionModeInput.builder()
                                        .portName("dataset")
                                        .inputType("DATASET")
                                        .file(datasetVersionUuid)
                                        .build()))
                                .build(),
                        ExecutionMode.builder()
                                .stepCount(2)
                                .experimentType("BINARY_CLASSIFICATION_EVALUATION")
                                .experimentConfigUuid(evaluationConfigUuid)
                                .inputs(List.of(ExecutionModeInput.builder()
                                        .portName("model")
                                        .inputType("ARTIFACT")
                                        .file("modelBundle")
                                        .sourceStepCount(1)
                                        .build()))
                                .build()
                ))
                .build();
        run.setUuid(runUuid);
        return run;
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

    private static ExperimentRunResolvedPlan resolvedPlan(OutputManifest output) {
        return ExperimentRunResolvedPlan.builder()
                .steps(List.of(ExperimentRunResolvedPlan.Step.builder()
                        .stepCount(1)
                        .experimentConfigUuid(FIRST_CONFIG_UUID)
                        .experimentType("CSV_PROFILE_ANALYSIS")
                        .inputs(List.of())
                        .outputs(List.of(ExperimentRunResolvedPlan.Output.builder()
                                .name(output.getName())
                                .dataKind(output.getDataKind().name())
                                .formatStrategy(output.getType().getType())
                                .format(output.getType().getFormat())
                                .sourceInputPort(output.getType().getSourceInputPort())
                                .downStreamPolicy(output.getDownStreamPolicy())
                                .requiredForRun(output.getRequired())
                                .build()))
                        .build()))
                .build();
    }

    private static ExperimentRunCompletedEvent completedEvent(List<ExperimentRunCompletedArtifact> artifacts) {
        return new ExperimentRunCompletedEvent(
                null,
                new ExperimentRunCompletedEventPayload(
                        null,
                        null,
                        null,
                        "SUCCEEDED",
                        new ExperimentRunCompletedResult(artifacts, List.of(), null),
                        null
                )
        );
    }

    private static ExperimentRunCompletedArtifact completedArtifact(
            Integer stepCount,
            String portName,
            String type,
            String format) {

        return new ExperimentRunCompletedArtifact(
                format,
                type,
                "s3://bucket/" + stepCount + "/" + portName,
                100L,
                "CSV_PROFILE_ANALYSIS",
                stepCount,
                portName
        );
    }
}
