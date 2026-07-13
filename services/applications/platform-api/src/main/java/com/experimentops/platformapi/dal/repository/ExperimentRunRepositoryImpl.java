package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.ExperimentRunListItem;
import com.experimentops.platformapi.model.ExperimentRunListItemProjection;
import com.experimentops.platformapi.model.ExperimentRunSearchCriteria;
import com.experimentops.platformapi.model.entity.ExecutionMode;
import com.experimentops.platformapi.model.entity.ExperimentRun;
import com.experimentops.platformapi.model.entity.RunDataset;
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
import java.util.List;

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
                experimentRun.<List<ExecutionMode>>get("executionMode"),
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
