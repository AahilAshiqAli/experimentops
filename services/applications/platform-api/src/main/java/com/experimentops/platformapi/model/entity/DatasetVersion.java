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
@Table(name="tbl_dataset_versions")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetVersion extends ExperimentOpsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "original_name")
    private String name;

    @Column(name = "storage_uri")
    private String storageUri;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "dataset_uuid")
    private String datasetUuid;

    @Column(name = "workspace_uuid")
    private String workspaceUuid;

    @Column(name ="status")
    @Convert(converter = StatusEnumConverter.class)
    private StatusEnum status;


    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatasetVersion)) return false;
        return (getUuid().equals(((DatasetVersion) o).getUuid()));
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(getUuid());
    }
}

