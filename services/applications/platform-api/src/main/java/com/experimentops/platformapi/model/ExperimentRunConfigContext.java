package com.experimentops.platformapi.model;

import com.experimentops.platformapi.model.entity.ExperimentTypeManifest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentRunConfigContext {
    private String experimentConfigUuid;
    private String experimentType;
    private JsonNode experimentConfigJson;
    private List<ExperimentTypeManifest> formatMappings;
    private Integer timeWeight;
}
