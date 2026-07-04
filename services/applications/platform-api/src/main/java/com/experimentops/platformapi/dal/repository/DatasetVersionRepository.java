package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.DatasetVersion;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, Long> {

    long countByDatasetUuidAndWorkspaceUuidAndStatusAndEnabled(String datasetUuid, String workspaceUuid, StatusEnum statusEnum, boolean enabled);

    Optional<DatasetVersion> findByUuidAndWorkspaceUuidAndEnabled(String uuid, String workspaceUuid, boolean enabled);

    Optional<DatasetVersion> findByUuidAndDatasetUuidAndWorkspaceUuidAndEnabled(String uuid, String datasetUuid, String workspaceUuid, boolean enabled);

    Optional<DatasetVersion> findByUuidAndDatasetUuidAndWorkspaceUuidAndStatusAndEnabled(String uuid, String datasetUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Page<DatasetVersion> findAllByDatasetUuidAndWorkspaceUuidAndStatusAndEnabledOrderByCreationDateDesc(String datasetUuid, String workspaceUuid, StatusEnum status, boolean enabled, Pageable pageable);

}
