package com.experimentops.utils.model.entity;

import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@MappedSuperclass
@Data
public abstract class ExperimentOpsEntity {

    @Column(name="uuid")
    protected String uuid;

    @CreationTimestamp
    @Column(name = "creation_date")
    protected Timestamp creationDate;

    @Column(name="created_by")
    protected String createdBy;

    @UpdateTimestamp
    @Column(name = "last_updated")
    protected Timestamp lastUpdated;

    @Column(name="updated_by")
    protected String updatedBy;

    @Column(name="enabled")
    protected boolean enabled;

    @Version
    protected int version;

    protected ExperimentOpsEntity() {
        createdBy = "0";
        updatedBy = "0";
        uuid = ExperimentOpsUtils.uuid();
        enabled = true;
    }

    protected ExperimentOpsEntity(ExperimentOpsHeaders headers) {
        createdBy = headers.getUserUuid();
        updatedBy = headers.getUserUuid();
        uuid = ExperimentOpsUtils.uuid();
        enabled = true;
    }

}
