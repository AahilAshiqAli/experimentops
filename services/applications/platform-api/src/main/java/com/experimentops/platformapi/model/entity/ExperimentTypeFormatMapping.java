package com.experimentops.platformapi.model.entity;

import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentTypeFormatMapping {
    private DatasetFileFormatEnum inputFormat;
    private DatasetFileFormatEnum outputFormat;
}
