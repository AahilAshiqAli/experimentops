package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.RunDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RunDatasetRepository extends JpaRepository<RunDataset, Long> {
}
