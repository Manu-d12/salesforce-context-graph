package org.autorabit.salesforcecontextgraph.repository;

import org.autorabit.salesforcecontextgraph.db_entities.SyncJob;
import org.autorabit.salesforcecontextgraph.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SyncJobRepository extends JpaRepository <SyncJob, Long> {
    Optional<SyncJob> findTopByOrgIdAndStatusOrderByCreatedDateDesc(String orgId, JobStatus status);
    Optional<SyncJob> findTopByOrgIdOrderByCreatedDateDesc(String orgId);
    List<SyncJob> findByOrgIdAndStatus(String orgId, JobStatus status);
}
