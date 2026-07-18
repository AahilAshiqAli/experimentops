package com.experimentops.platformapi.model.entity;

import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class FormatStrategy {
    private FormatStrategyTypeEnum type;
    private String sourceInputPort;
    private DatasetFileFormatEnum format;
}
