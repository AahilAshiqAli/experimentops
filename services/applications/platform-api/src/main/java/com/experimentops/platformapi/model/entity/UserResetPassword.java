package com.experimentops.platformapi.model.entity;

import com.experimentops.utils.model.entity.ExperimentOpsEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name="tbl_user_reset_password")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResetPassword extends ExperimentOpsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "user_uuid")
    private String userUuid;

    @Column(name = "email")
    private String email;

    @Column(name = "workspace_uuid")
    private String workspaceUuid;

    @Column(name = "workspace_name")
    private String workspaceName;

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserResetPassword)) return false;
        return (getUuid().equals(((UserResetPassword) o).getUuid()));
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(getUuid());
    }

}
