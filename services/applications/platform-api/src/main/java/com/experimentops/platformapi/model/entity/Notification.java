package com.experimentops.platformapi.model.entity;

import com.experimentops.platformapi.model.type.NotificationProviderEnum;
import com.experimentops.platformapi.model.type.NotificationStatusEnum;
import com.experimentops.platformapi.model.type.converter.NotificationProviderEnumConverter;
import com.experimentops.platformapi.model.type.converter.NotificationStatusEnumConverter;
import com.experimentops.utils.model.entity.ExperimentOpsEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "tbl_notification")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends ExperimentOpsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "workspace_uuid")
    private String workspaceUuid;

    @Column(name = "recipient")
    private String recipient;

    @Column(name = "provider")
    @Convert(converter = NotificationProviderEnumConverter.class)
    private NotificationProviderEnum provider;

    @Column(name = "status")
    @Convert(converter = NotificationStatusEnumConverter.class)
    private NotificationStatusEnum status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count")
    private int retryCount;

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification)) return false;
        return (getUuid().equals(((Notification) o).getUuid()));
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(getUuid());
    }
}
