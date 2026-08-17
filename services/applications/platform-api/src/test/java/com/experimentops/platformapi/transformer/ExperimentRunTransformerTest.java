package com.experimentops.platformapi.transformer;

import com.experimentops.experiment.run.model.v1.ExperimentRunDetailResponseModel;
import com.experimentops.platformapi.model.ExperimentRunDetailSummary;
import com.experimentops.platformapi.model.entity.ExecutionMode;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExperimentRunTransformerTest {

    @Test
    void detailResponseInfersCompletedStepsForLegacySucceededRun() {
        ExperimentRunDetailSummary summary = summary(
                ExperimentStatusEnum.SUCCEEDED,
                null,
                List.of(
                        ExecutionMode.builder().stepCount(1).experimentConfigUuid("config-1").build(),
                        ExecutionMode.builder().stepCount(2).experimentConfigUuid("config-2").build()
                )
        );

        ExperimentRunDetailResponseModel response = transform(summary);

        assertThat(response.getCompletedSteps()).isEqualTo(2);
    }

    @Test
    void detailResponseDefaultsCompletedStepsForLegacyPendingRun() {
        ExperimentRunDetailSummary summary = summary(ExperimentStatusEnum.PENDING, null, List.of());

        ExperimentRunDetailResponseModel response = transform(summary);

        assertThat(response.getCompletedSteps()).isZero();
    }

    private static ExperimentRunDetailResponseModel transform(ExperimentRunDetailSummary summary) {
        return new ExperimentRunTransformer().transformExperimentRunDetailResponseModel(
                summary,
                List.of(),
                List.of(),
                Map.of("config-1", "CSV_PROFILE_ANALYSIS", "config-2", "CSV_PROFILE_ANALYSIS"),
                Map.of(),
                List.of(),
                new ExperimentOpsHeaders()
        );
    }

    private static ExperimentRunDetailSummary summary(
            ExperimentStatusEnum status,
            @Nullable Integer completedSteps,
            List<ExecutionMode> executionMode) {
        return new ExperimentRunDetailSummary(
                "run-1",
                "Run 1",
                status,
                null,
                "experiment-1",
                "Experiment 1",
                "project-1",
                "Project 1",
                Timestamp.valueOf("2026-01-01 00:00:00"),
                Timestamp.valueOf("2026-01-01 00:01:00"),
                "user-1",
                0L,
                completedSteps,
                executionMode
        );
    }
}
