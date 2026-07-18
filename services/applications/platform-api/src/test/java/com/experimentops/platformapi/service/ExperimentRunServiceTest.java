package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.platformapi.dal.repository.ExperimentRepository;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.validator.ExperimentRunValidator;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExperimentRunServiceTest {

    @Test
    void getExperimentRequiresActiveExperiment() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        ExperimentRunService service = new ExperimentRunService(
                null,
                new ExperimentRunValidator(),
                null,
                experimentRepository,
                null,
                null,
                null,
                null,
                null
        );
        ExperimentOpsHeaders headers = new ExperimentOpsHeaders();
        headers.setWorkspaceUuid("workspace-1");

        when(experimentRepository.findByUuidAndWorkspaceUuidAndStatusAndEnabled(
                "experiment-1",
                "workspace-1",
                StatusEnum.ACTIVE,
                true
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExperiment("experiment-1", headers))
                .isInstanceOf(EntityNotFoundException.class);

        verify(experimentRepository).findByUuidAndWorkspaceUuidAndStatusAndEnabled(
                "experiment-1",
                "workspace-1",
                StatusEnum.ACTIVE,
                true
        );
    }
}
