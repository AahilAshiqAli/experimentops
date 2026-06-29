package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.ProjectSummary;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectSummaryRepository extends JpaRepository<ProjectSummary, Long> {

    Optional<ProjectSummary> findByProjectUuidAndWorkspaceUuidAndStatusAndEnabled(String projectUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    List<ProjectSummary> findAllByWorkspaceUuidAndStatusAndEnabledOrderByCreatedAtDesc(String workspaceUuid, StatusEnum status, boolean enabled);
}
