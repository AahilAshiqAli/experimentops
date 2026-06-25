package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.RunArtifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RunArtifactRepository extends JpaRepository<RunArtifact, Long> {
}
