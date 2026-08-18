package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.ArtifactType;
import com.experimentops.platformapi.model.entity.DownStreamPolicyEnum;
import com.experimentops.platformapi.model.entity.RunArtifact;
import com.experimentops.platformapi.model.entity.RunArtifactStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RunArtifactRepository extends JpaRepository<RunArtifact, Long> {

    List<RunArtifact> findByExperimentRunUuidAndWorkspaceUuidAndDownStreamPolicyInAndEnabledOrderByStepCountAscPortNameAsc(
            String experimentRunUuid,
            String workspaceUuid,
            List<DownStreamPolicyEnum> downStreamPolicyEnums,
            boolean enabled);

    List<RunArtifact> findByExperimentRunUuidAndWorkspaceUuidAndEnabledOrderByStepCountAscPortNameAsc(
            String experimentRunUuid,
            String workspaceUuid,
            boolean enabled);

    List<RunArtifact> findByExperimentRunUuidAndWorkspaceUuidAndArtifactTypeAndStatusAndEnabled(
            String experimentRunUuid,
            String workspaceUuid,
            ArtifactType artifactType,
            RunArtifactStatusEnum status,
            boolean enabled);

    Optional<RunArtifact> findByUuidAndWorkspaceUuidAndEnabled(String uuid, String workspaceUuid, boolean enabled);
}
