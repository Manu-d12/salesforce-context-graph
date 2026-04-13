package org.autorabit.salesforcecontextgraph.service;

import lombok.AllArgsConstructor;
import org.autorabit.salesforcecontextgraph.db_entities.MetadataDependency;
import org.autorabit.salesforcecontextgraph.db_entities.SyncJob;
import org.autorabit.salesforcecontextgraph.domain.enums.JobStatus;
import org.autorabit.salesforcecontextgraph.repository.MetadataDependencyRepository;
import org.autorabit.salesforcecontextgraph.repository.SyncJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
public class SyncJobTransactionService {

    private final SyncJobRepository syncJobRepository;
    private final MetadataDependencyRepository metadataDependencyRepository;

    @Transactional
    public void initializeSync(String orgId) {
        List<MetadataDependency> orgDependencies = metadataDependencyRepository.findByOrgId(orgId);
        if (orgDependencies != null && !orgDependencies.isEmpty()) {
            metadataDependencyRepository.deleteAll(orgDependencies);
        }

        SyncJob latestCompletedJob = syncJobRepository
                .findTopByOrgIdAndStatusOrderByCreatedDateDesc(orgId, JobStatus.COMPLETED)
                .orElse(null);

        if (latestCompletedJob != null) {
            latestCompletedJob.setStatus(JobStatus.OUTDATED);
            syncJobRepository.save(latestCompletedJob);
        }

        SyncJob syncJob = SyncJob.builder()
                .createdDate(new Date())
                .status(JobStatus.IN_PROGRESS)
                .orgId(orgId)
                .build();

        syncJobRepository.save(syncJob);
    }

    @Transactional
    public void markLatestInProgressJobAsCompleted(String orgId) {
        SyncJob syncJob = syncJobRepository
                .findTopByOrgIdAndStatusOrderByCreatedDateDesc(orgId, JobStatus.IN_PROGRESS)
                .orElse(null);

        if (syncJob != null) {
            syncJob.setStatus(JobStatus.COMPLETED);
            syncJobRepository.save(syncJob);
        }
    }

    @Transactional
    public void markLatestInProgressJobAsFailed(String orgId) {
        SyncJob syncJob = syncJobRepository
                .findTopByOrgIdAndStatusOrderByCreatedDateDesc(orgId, JobStatus.IN_PROGRESS)
                .orElse(null);

        if (syncJob != null) {
            syncJob.setStatus(JobStatus.FAILED);
            syncJobRepository.save(syncJob);
        }
    }

    @Transactional
    public boolean isSyncInProgress(String orgId) {
        SyncJob syncJob = syncJobRepository
                .findTopByOrgIdAndStatusOrderByCreatedDateDesc(orgId, JobStatus.IN_PROGRESS)
                .orElse(null);
        return syncJob != null;
    }
}