package com.experimentops.platformapi.model.entity;

import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.model.type.converter.StatusEnumConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "vu_project_summary")
@Immutable
public class ProjectSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "project_uuid")
    private String projectUuid;

    @Column(name = "workspace_uuid")
    private String workspaceUuid;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    @Convert(converter = StatusEnumConverter.class)
    private StatusEnum status;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "dataset_count")
    private long datasetCount;

    @Column(name = "experiment_count")
    private long experimentCount;

    @Column(name = "experiment_config_count")
    private long experimentConfigCount;

    @Column(name = "dataset_version_count")
    private long datasetVersionCount;

    @Column(name = "experiment_run_count")
    private long experimentRunCount;

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectSummary)) return false;
        return getProjectUuid().equals(((ProjectSummary) o).getProjectUuid());
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(getProjectUuid());
    }
}
