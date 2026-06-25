package com.experimentops.platformapi.model.entity;

import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.platformapi.model.type.converter.ExperimentStatusEnumConverter;
import com.experimentops.utils.model.entity.ExperimentOpsEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name="tbl_experiment_runs")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentRun extends ExperimentOpsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "experiment_uuid")
    private String experimentUuid;

    @Column(name = "experiment_config_uuid")
    private String experimentConfigUuid;

    @Column(name = "workspace_uuid")
    private String workspaceUuid;

    @Column(name = "experiment_status")
    @Convert(converter = ExperimentStatusEnumConverter.class)
    private ExperimentStatusEnum experimentStatus;

    @Column(name = "progress")
    private int progress;

    @Column(name = "run_number")
    private int runNumber;

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExperimentRun)) return false;
        return (getUuid().equals(((ExperimentRun) o).getUuid()));
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(getUuid());
    }
}
