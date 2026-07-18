package com.experimentops.platformapi.model.entity;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class InputRelationship {
    private InputRelationshipTypeEnum type;
    private List<String> ports;
}
