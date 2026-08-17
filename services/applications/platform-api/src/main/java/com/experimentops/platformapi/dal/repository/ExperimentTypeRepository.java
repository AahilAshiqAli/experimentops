package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.ExperimentType;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentTypeRepository extends JpaRepository<ExperimentType, Long> {

    Optional<ExperimentType> findByUuidAndWorkspaceUuidAndEnabled(String uuid, String workspaceUuid, boolean enabled);

    Optional<ExperimentType> findByUuidAndStatusAndWorkspaceUuidAndEnabled(String uuid, StatusEnum status, String workspaceUuid, boolean enabled);

    Optional<ExperimentType> findByNameAndStatusAndWorkspaceUuidAndEnabled(String name, StatusEnum status, String workspaceUuid, boolean enabled);

    @Query("""
    SELECT e
    FROM ExperimentType e
    WHERE e.status = :status
      AND e.enabled = :enabled
      AND (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')))
      AND (e.workspaceUuid IS NULL OR e.workspaceUuid = :workspaceUuid)
    ORDER BY e.lastUpdated DESC
""")
    Page<ExperimentType> findAllByStatusAndWorkspaceUuidAndEnabled(
            @Param("status") StatusEnum status,
            @Param("workspaceUuid") String workspaceUuid,
            @Param("enabled") boolean enabled,
            @Param("name") String name,
            Pageable pageable
    );
}
