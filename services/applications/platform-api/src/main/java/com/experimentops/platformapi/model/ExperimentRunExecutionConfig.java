package com.experimentops.platformapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentRunExecutionConfig {
    private Integer stepCount;
    private String experimentType;
    private JsonNode experimentConfigJson;
    private Integer timeWeight;
}
