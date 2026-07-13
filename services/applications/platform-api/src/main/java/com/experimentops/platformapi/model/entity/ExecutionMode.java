package com.experimentops.platformapi.model.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionMode {
    private Integer stepCount;
    private String experimentConfigUuid;
}
