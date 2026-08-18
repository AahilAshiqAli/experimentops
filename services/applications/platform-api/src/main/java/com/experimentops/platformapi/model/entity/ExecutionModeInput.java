package com.experimentops.platformapi.model.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExecutionModeInput {
    private String portName;
    private String inputType;
    private String file;
    private Integer sourceStepCount;
}
