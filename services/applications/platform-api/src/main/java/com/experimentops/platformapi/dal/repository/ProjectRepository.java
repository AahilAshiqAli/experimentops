package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.Project;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByUuidAndWorkspaceUuidAndEnabled(String uuid, String workspaceUuid, boolean enabled);

    Optional<Project> findByUuidAndWorkspaceUuidAndStatusAndEnabled(String uuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Optional<Project> findByNameAndWorkspaceUuidAndStatusAndEnabled(String name, String workspaceUuid, StatusEnum status, boolean enabled);

    Page<Project> findAllByWorkspaceUuidAndStatusAndEnabledOrderByCreationDateDesc(String workspaceUuid, StatusEnum status, boolean enabled, Pageable pageable);

}
