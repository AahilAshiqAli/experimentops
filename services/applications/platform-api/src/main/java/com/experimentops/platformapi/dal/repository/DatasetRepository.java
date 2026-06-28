package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.Dataset;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    Optional<Dataset> findByUuidAndProjectUuidAndWorkspaceUuidAndEnabled(String uuid, String projectUuid, String workspaceUuid, boolean enabled);

    Optional<Dataset> findByUuidAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(String uuid, String projectUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Optional<Dataset> findByNameAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(String name, String projectUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Optional<Dataset> findByUuidAndWorkspaceUuidAndStatusAndEnabled(String uuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Page<Dataset> findAllByProjectUuidAndWorkspaceUuidAndStatusAndEnabledOrderByLastUpdatedDesc(String projectUuid, String workspaceUuid, StatusEnum status, boolean enabled, Pageable pageable);

}
