package com.experimentops.platformapi.model.entity;

import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.model.type.converter.StatusEnumConverter;
import com.experimentops.utils.ExperimentOpsJsonType;
import com.experimentops.utils.model.entity.ExperimentOpsEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "tbl_experiment_config")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentConfig extends ExperimentOpsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "experiment_type")
    private String experimentType;

    @Type(ExperimentOpsJsonType.class)
    @Column(columnDefinition = "json", name = "config")
    private JsonNode config;

    @Column(name = "experiment_uuid")
    private String experimentUuid;

    @Column(name = "workspace_uuid")
    private String workspaceUuid;

    @Column(name = "status")
    @Convert(converter = StatusEnumConverter.class)
    private StatusEnum status;

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExperimentConfig)) return false;
        return (getUuid().equals(((ExperimentConfig) o).getUuid()));
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(getUuid());
    }
}
