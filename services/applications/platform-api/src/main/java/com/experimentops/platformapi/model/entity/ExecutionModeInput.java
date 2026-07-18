package com.experimentops.platformapi.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionModeInput {
    private String portName;
    private String inputType;
    private String file;
    private Integer sourceStepCount;
}
