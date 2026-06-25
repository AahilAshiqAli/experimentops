package com.experimentops.platformapi.model.entity;

import com.experimentops.utils.model.entity.ExperimentOpsEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name="tbl_run_artifact")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunArtifact extends ExperimentOpsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "experiment_run_uuid")
    private String experimentRunUuid;

    @Column(name = "workspace_uuid")
    private String workspaceUuid;

    @Column(name = "artifact_type")
    private String artifactType;

    @Column(name = "storage_uri")
    private String storageUri;

    @Column(name = "mime_type")
    private String mimeType;

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RunArtifact)) return false;
        return (getUuid().equals(((RunArtifact) o).getUuid()));
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(getUuid());
    }

}
