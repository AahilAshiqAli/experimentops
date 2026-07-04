package com.experimentops.platformapi.model.entity;

import com.experimentops.platformapi.model.type.DatasetScanStatusEnum;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.model.type.converter.DatasetScanStatusEnumConverter;
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

    @Column(name = "format")
    private String format;

    @Column(name = "size_bytes")
    private Long size;

    @Column(name = "dataset_uuid")
    private String datasetUuid;

    @Column(name = "workspace_uuid")
    private String workspaceUuid;

    @Column(name ="status")
    @Convert(converter = StatusEnumConverter.class)
    private StatusEnum status;

    @Column(name = "scan_status")
    @Convert(converter = DatasetScanStatusEnumConverter.class)
    private DatasetScanStatusEnum scanStatus;

    @Column(name = "scan_message")
    private String scanMessage;

    @Column(name = "preview_uri")
    private String previewUri;

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
