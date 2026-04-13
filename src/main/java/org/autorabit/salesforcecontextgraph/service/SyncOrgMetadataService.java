package org.autorabit.salesforcecontextgraph.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomApplicationDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomPermissionDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomStandardObjectDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomTabDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.MetadataComponentDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetGroupDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.ProfileDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.RoleDependenciesCollector;
import org.autorabit.salesforcecontextgraph.db_entities.SyncJob;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceOAuthService;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;
import org.autorabit.salesforcecontextgraph.repository.SyncJobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@AllArgsConstructor
@Slf4j
public class SyncOrgMetadataService {

    private final CustomApplicationDependenciesCollector customApplicationDependenciesCollector;
    private final CustomPermissionDependenciesCollector customPermissionDependenciesCollector;
    private final CustomStandardObjectDependencyCollector customStandardObjectDependencyCollector;
    private final CustomTabDependenciesCollector customTabDependenciesCollector;
    private final MetadataComponentDependencyCollector metadataComponentDependencyCollector;
    private final SalesforceOAuthService salesforceOAuthService;
    private final PermissionSetDependenciesCollector permissionSetDependenciesCollector;
    private final PermissionSetGroupDependenciesCollector permissionSetGroupDependenciesCollector;
    private final ProfileDependenciesCollector profileDependenciesCollector;
    private final RoleDependenciesCollector roleDependenciesCollector;
    private final MetadataApiClient apiClient;
    private final SyncJobRepository syncJobRepository;
    private final SyncJobTransactionService syncJobTransactionService;

    @Qualifier("loadDependenciesExecutor")
    private final ThreadPoolTaskExecutor loadDependenciesExecutor;

    @Async("loadDependenciesExecutor")
    public void sync(SfOrgSyncRequestDto requestDto, SalesforceSession session, String orgId) {

        try {
            log.info("Starting metadata sync for orgId={}", orgId);


            syncJobTransactionService.initializeSync(orgId);


            CompletableFuture<Void> metadataComponentTask = runCollector(
                    () -> metadataComponentDependencyCollector.persistRelativeGraphEdges(requestDto, session)
            );
            CompletableFuture<Void> customStandardObjectTask = runCollector(
                    () -> customStandardObjectDependencyCollector.persistRelativeGraphEdges(requestDto, session)
            );
            CompletableFuture<Void> customApplicationTask = runCollector(
                    () -> customApplicationDependenciesCollector.persistRelativeGraphEdges(requestDto, session)
            );
            CompletableFuture<Void> customPermissionTask = runCollector(
                    () -> customPermissionDependenciesCollector.persistRelativeGraphEdges(requestDto, session)
            );
            CompletableFuture<Void> customTabTask = runCollector(
                    () -> customTabDependenciesCollector.persistRelativeGraphEdges(requestDto, session)
            );
            CompletableFuture<Void> permissionSetTask = runCollector(
                    () -> permissionSetDependenciesCollector.persistRelativeGraphEdges(requestDto, session)
            );
            CompletableFuture<Void> permissionSetGroupTask = runCollector(
                    () -> permissionSetGroupDependenciesCollector.persistRelativeGraphEdges(requestDto, session)
            );
            CompletableFuture<Void> roleTask = runCollector(
                    () -> roleDependenciesCollector.persistRelativeGraphEdges(requestDto, session)
            );

            CompletableFuture.allOf(
                    metadataComponentTask,
                    customStandardObjectTask,
                    customApplicationTask,
                    customPermissionTask,
                    customTabTask,
                    permissionSetTask,
                    permissionSetGroupTask,
                    roleTask
            ).join();


            Thread.sleep(1000);

            syncJobTransactionService.markLatestInProgressJobAsCompleted(orgId);
            log.info("Metadata sync completed successfully for orgId={}", orgId);

        } catch (CompletionException e) {
            log.error("Collector failed for orgId={}", orgId, e);
            if (orgId != null) {
                syncJobTransactionService.markLatestInProgressJobAsFailed(orgId);
            }
        } catch (Exception e) {
            log.error("Something went wrong for sync job. orgId={}", orgId, e);
            if (orgId != null) {
                syncJobTransactionService.markLatestInProgressJobAsFailed(orgId);
            }
        }
    }

    private CompletableFuture<Void> runCollector(Runnable collectorTask) {
        return CompletableFuture.runAsync(() -> {
            try {
                collectorTask.run();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, loadDependenciesExecutor);
    }

    @Transactional(readOnly = true)
    public String getLastSyncInJobStatus(String orgId) {
        SyncJob latestJob = syncJobRepository
                .findTopByOrgIdOrderByCreatedDateDesc(orgId)
                .orElse(null);

        if (latestJob == null) {
            return "No Sync Found";
        }

        return switch (latestJob.getStatus()) {
            case IN_PROGRESS -> "Sync in Progress";
            case COMPLETED -> "Sync Completed";
            case FAILED -> "Sync Failed";
            case OUTDATED -> "Sync Outdated";
            default -> "Unknown Status";
        };
    }
}