package com.experimentops.platformapi.model.entity;

import com.experimentops.utils.model.entity.ExperimentOpsEntity;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name="tbl_experiment_run_logs")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentRunLog extends ExperimentOpsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "experiment_run_uuid")
    private String experimentRunUuid;

    @Column(name = "sequence")
    private long sequence;

    @Column(name = "timestamp")
    private Timestamp timestamp;

    @Column(name = "level")
    private String level;

    @Column(name = "experiment_type")
    private String experimentType;

    @Column(name = "message")
    private String message;

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExperimentRunLog)) return false;
        return (getUuid().equals(((ExperimentRunLog) o).getUuid()));
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(getUuid());
    }

}
