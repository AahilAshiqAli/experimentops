package com.experimentops.platformapi.model.entity;

import com.experimentops.utils.model.entity.ExperimentOpsEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name="tbl_dataset_runs")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunDataset extends ExperimentOpsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "experiment_run_uuid")
    private String experimentRunUuid;

    @Column(name = "dataset_version_uuid")
    private String datasetVersionUuid;

    @Column(name = "workspace_uuid")
    private String workspaceUuid;

    @Column(name = "usage_type")
    private String usage;

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RunDataset)) return false;
        return (getUuid().equals(((RunDataset) o).getUuid()));
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(getUuid());
    }

}
