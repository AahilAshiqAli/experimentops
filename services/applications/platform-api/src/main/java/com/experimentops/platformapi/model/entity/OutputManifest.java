package com.experimentops.platformapi.model.entity;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class OutputManifest {
    private String name;
    private Boolean required;
    private OutputDataKindEnum dataKind;
    private FormatStrategy type;
    private DownStreamPolicyEnum downStreamPolicy;
}
