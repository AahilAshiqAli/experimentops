package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.DatasetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, Long> {

    long countByDatasetUuidAndWorkspaceUuidAndEnabled(String datasetUuid, String workspaceUuid, boolean enabled);

    List<DatasetVersion> findAllByDatasetUuidAndWorkspaceUuidAndEnabledOrderByCreationDateDesc(String datasetUuid, String workspaceUuid, boolean enabled);

    Optional<DatasetVersion> findByUuidAndWorkspaceUuidAndEnabled(String uuid, String workspaceUuid, boolean enabled);

}
