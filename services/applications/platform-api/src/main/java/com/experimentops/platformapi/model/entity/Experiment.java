package com.experimentops.platformapi.model.entity;

import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.model.type.converter.StatusEnumConverter;
import com.experimentops.utils.model.entity.ExperimentOpsEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name="tbl_experiment")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experiment extends ExperimentOpsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "experiment_type")
    private String experimentType;

    @Column(name = "project_uuid")
    private String projectUuid;

    @Column(name = "workspace_uuid")
    private String workspaceUuid;

    @Column(name ="status")
    @Convert(converter = StatusEnumConverter.class)
    private StatusEnum status;


    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Experiment)) return false;
        return (getUuid().equals(((Experiment) o).getUuid()));
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(getUuid());
    }
}


