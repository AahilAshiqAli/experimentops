package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.ExperimentRunListItem;
import com.experimentops.platformapi.model.ExperimentRunDetailArtifact;
import com.experimentops.platformapi.model.ExperimentRunDetailConfigType;
import com.experimentops.platformapi.model.ExperimentRunDetailDataset;
import com.experimentops.platformapi.model.ExperimentRunDetailSummary;
import com.experimentops.platformapi.model.ExperimentRunListItemProjection;
import com.experimentops.platformapi.model.ExperimentRunSearchCriteria;
import com.experimentops.platformapi.model.entity.ExperimentRun;
import com.experimentops.platformapi.model.entity.RunDataset;
import com.experimentops.platformapi.model.entity.RunArtifactStatusEnum;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.utils.ExperimentOpsUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ExperimentRunRepositoryImpl implements ExperimentRunRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @NonNull
    public Page<ExperimentRunListItemProjection> searchExperimentRuns(
            @NonNull ExperimentRunSearchCriteria criteria,
            @NonNull Pageable pageable
    ) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ExperimentRunListItemProjection> query = criteriaBuilder.createQuery(ExperimentRunListItemProjection.class);
        Root<ExperimentRun> experimentRun = query.from(ExperimentRun.class);

        Subquery<Long> datasetCount = buildDatasetCountSubquery(query, criteriaBuilder, experimentRun, criteria);
        query.select(criteriaBuilder.construct(
                ExperimentRunListItem.class,
                experimentRun.<String>get("uuid"),
                experimentRun.<String>get("name"),
                experimentRun.<Integer>get("progress"),
                datasetCount,
                experimentRun.get("experimentStatus"),
                experimentRun.get("creationDate"),
                experimentRun.get("lastUpdated")
        ));
        query.where(buildPredicates(criteriaBuilder, experimentRun, criteria));
        query.orderBy(criteriaBuilder.desc(experimentRun.get("lastUpdated")));

        TypedQuery<ExperimentRunListItemProjection> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(typedQuery.getResultList(), pageable, countExperimentRuns(criteriaBuilder, criteria));
    }

    @Override
    @NonNull
    public Optional<ExperimentRunDetailSummary> findExperimentRunDetailSummary(
            @NonNull String experimentRunUuid,
            @NonNull String workspaceUuid) {

        List<Object[]> result = entityManager.createQuery("""
                        SELECT
                            er,
                            e.uuid,
                            e.name,
                            p.uuid,
                            p.name,
                            TRIM(
                                CONCAT(
                                    COALESCE(u.firstName, ''),
                                    ' ',
                                    COALESCE(u.lastName, '')
                                )
                            ) as createdBy,
                            (
                                SELECT COUNT(ra)
                                FROM RunArtifact ra
                                WHERE ra.experimentRunUuid = er.uuid
                                  AND ra.workspaceUuid = :workspaceUuid
                                  AND ra.enabled = true
                            )
                        FROM ExperimentRun er
                        JOIN Experiment e
                          ON e.uuid = er.experimentUuid
                         AND e.workspaceUuid = er.workspaceUuid
                         AND e.status = :activeStatus
                         AND e.enabled = true
                        JOIN Project p
                          ON p.uuid = e.projectUuid
                         AND p.workspaceUuid = er.workspaceUuid
                         AND p.status = :activeStatus
                         AND p.enabled = true
                        LEFT JOIN User u
                          ON er.createdBy = u.uuid
                          AND u.status = :activeStatus
                          AND u.enabled = true
                        WHERE er.uuid = :experimentRunUuid
                          AND er.workspaceUuid = :workspaceUuid
                          AND er.enabled = true
                        """, Object[].class)
                .setParameter("experimentRunUuid", experimentRunUuid)
                .setParameter("workspaceUuid", workspaceUuid)
                .setParameter("activeStatus", StatusEnum.ACTIVE)
                .getResultList();

        return result.stream()
                .findFirst()
                .map(row -> {
                    ExperimentRun experimentRun = (ExperimentRun) row[0];
                    return new ExperimentRunDetailSummary(
                            experimentRun.getUuid(),
                            experimentRun.getName(),
                            experimentRun.getExperimentStatus(),
                            experimentRun.getMessage(),
                            (String) row[1],
                            (String) row[2],
                            (String) row[3],
                            (String) row[4],
                            experimentRun.getCreationDate(),
                            experimentRun.getLastUpdated(),
                            (String) row[5],
                            (Long) row[6],
                            experimentRun.getExecutionMode()
                    );
                });
    }

    @Override
    @NonNull
    public List<ExperimentRunDetailDataset> findExperimentRunDetailDatasets(
            @NonNull String experimentRunUuid,
            @NonNull String workspaceUuid) {

        return entityManager.createQuery("""
                        SELECT new com.experimentops.platformapi.model.ExperimentRunDetailDataset(
                            rd.datasetVersionUuid,
                            dv.name
                        )
                        FROM RunDataset rd
                        JOIN DatasetVersion dv
                          ON dv.uuid = rd.datasetVersionUuid
                         AND dv.workspaceUuid = rd.workspaceUuid
                         AND dv.status = :activeStatus
                         AND dv.enabled = true
                        WHERE rd.experimentRunUuid = :experimentRunUuid
                          AND rd.workspaceUuid = :workspaceUuid
                          AND rd.enabled = true
                        """, ExperimentRunDetailDataset.class)
                .setParameter("experimentRunUuid", experimentRunUuid)
                .setParameter("workspaceUuid", workspaceUuid)
                .setParameter("activeStatus", StatusEnum.ACTIVE)
                .getResultList();
    }

    @Override
    @NonNull
    public List<ExperimentRunDetailArtifact> findExperimentRunDetailPrimaryArtifacts(
            @NonNull String experimentRunUuid,
            @NonNull String workspaceUuid) {

        return entityManager.createQuery("""
                        SELECT new com.experimentops.platformapi.model.ExperimentRunDetailArtifact(
                            ra.uuid,
                            ra.portName
                        )
                        FROM RunArtifact ra
                        WHERE ra.experimentRunUuid = :experimentRunUuid
                          AND ra.workspaceUuid = :workspaceUuid
                          AND ra.status = :primaryStatus
                          AND ra.enabled = true
                        """, ExperimentRunDetailArtifact.class)
                .setParameter("experimentRunUuid", experimentRunUuid)
                .setParameter("workspaceUuid", workspaceUuid)
                .setParameter("primaryStatus", RunArtifactStatusEnum.PRIMARY)
                .getResultList();
    }

    @Override
    @NonNull
    public List<ExperimentRunDetailConfigType> findExperimentRunDetailConfigTypes(
            @NonNull Collection<String> experimentConfigUuids,
            @NonNull String experimentUuid,
            @NonNull String workspaceUuid) {

        if (experimentConfigUuids.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery("""
                        SELECT new com.experimentops.platformapi.model.ExperimentRunDetailConfigType(
                            ec.uuid,
                            ec.experimentType
                        )
                        FROM ExperimentConfig ec
                        WHERE ec.uuid IN (:experimentConfigUuids)
                          AND ec.experimentUuid = :experimentUuid
                          AND ec.workspaceUuid = :workspaceUuid
                          AND ec.status = :activeStatus
                          AND ec.enabled = true
                        """, ExperimentRunDetailConfigType.class)
                .setParameter("experimentConfigUuids", experimentConfigUuids)
                .setParameter("experimentUuid", experimentUuid)
                .setParameter("workspaceUuid", workspaceUuid)
                .setParameter("activeStatus", StatusEnum.ACTIVE)
                .getResultList();
    }

    private Subquery<Long> buildDatasetCountSubquery(
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder,
            Root<ExperimentRun> experimentRun,
            ExperimentRunSearchCriteria criteria
    ) {
        Subquery<Long> datasetCount = query.subquery(Long.class);
        Root<RunDataset> runDataset = datasetCount.from(RunDataset.class);
        datasetCount.select(criteriaBuilder.count(runDataset));
        datasetCount.where(
                criteriaBuilder.equal(runDataset.get("experimentRunUuid"), experimentRun.get("uuid")),
                criteriaBuilder.equal(runDataset.get("workspaceUuid"), criteria.workspaceUuid()),
                criteriaBuilder.isTrue(runDataset.get("enabled"))
        );
        return datasetCount;
    }

    private long countExperimentRuns(
            CriteriaBuilder criteriaBuilder,
            ExperimentRunSearchCriteria criteria
    ) {
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<ExperimentRun> experimentRun = countQuery.from(ExperimentRun.class);
        countQuery.select(criteriaBuilder.count(experimentRun));
        countQuery.where(buildPredicates(criteriaBuilder, experimentRun, criteria));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder criteriaBuilder,
            Root<ExperimentRun> experimentRun,
            ExperimentRunSearchCriteria criteria
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(experimentRun.get("experimentUuid"), criteria.experimentUuid()));
        predicates.add(criteriaBuilder.equal(experimentRun.get("workspaceUuid"), criteria.workspaceUuid()));
        predicates.add(criteriaBuilder.isTrue(experimentRun.get("enabled")));

        if (!StringUtils.isBlank(criteria.name())) {
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(experimentRun.get("name")),
                    "%" + criteria.name().toLowerCase() + "%"
            ));
        }
        if (!ExperimentOpsUtils.isEmpty(criteria.statuses())) {
            predicates.add(experimentRun.get("experimentStatus").in(criteria.statuses()));
        }
        return predicates.toArray(Predicate[]::new);
    }
}
