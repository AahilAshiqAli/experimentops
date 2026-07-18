package com.experimentops.platformapi.model.entity;

import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import lombok.*;
import org.apache.kafka.common.protocol.types.Field;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class InputManifest {
    private String portName;
    private Boolean required;
    private String cardinality;
    private InputContract contract;
}



