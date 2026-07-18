package com.experimentops.platformapi.model.entity;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentTypeManifest {
    private List<InputManifest> inputs;
    private List<InputRelationship> inputRelationships;
    private List<OutputManifest> outputs;
}
