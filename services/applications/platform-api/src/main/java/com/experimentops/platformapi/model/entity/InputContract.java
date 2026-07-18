package com.experimentops.platformapi.model.entity;

import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class InputContract {
    private String dataKind;
    private List<DatasetFileFormatEnum> acceptedFormats;
}
