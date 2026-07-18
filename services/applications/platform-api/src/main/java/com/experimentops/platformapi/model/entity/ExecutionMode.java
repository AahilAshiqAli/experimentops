package com.experimentops.platformapi.model.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionMode {
    private Integer stepCount;
    private String experimentConfigUuid;
    private List<ExecutionModeInput> inputs;
}
