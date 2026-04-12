package org.autorabit.salesforcecontextgraph.service;

import lombok.AllArgsConstructor;
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
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceOAuthService;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
public class SyncOrgMetadataService {

    private final CustomApplicationDependenciesCollector customApplicationDependenciesCollector;
    private final CustomPermissionDependenciesCollector customPermissionDependenciesCollector;
    private final CustomStandardObjectDependencyCollector customStandardObjectDependencyCollector;
    private final CustomTabDependenciesCollector customTabDependenciesCollector;
    private final MetadataComponentDependencyCollector metadataComponentDependencyCollector;
    private final SalesforceOAuthService salesforceOAuthService;
    private PermissionSetDependenciesCollector permissionSetDependenciesCollector;
    private PermissionSetGroupDependenciesCollector permissionSetGroupDependenciesCollector;
    private ProfileDependenciesCollector profileDependenciesCollector;
    private RoleDependenciesCollector roleDependenciesCollector;
    @Qualifier("loadDependenciesExecutor")
    private final ThreadPoolTaskExecutor loadDependenciesExecutor;

    @Async("loadDependenciesExecutor")
    public void sync(SfOrgSyncRequestDto requestDto) {
        try {
            SalesforceSession session = salesforceOAuthService.authenticate(requestDto);

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
//        CompletableFuture<Void> profileTask = runCollector(
//                () -> profileDependenciesCollector.persistRelativeGraphEdges(requestDto, session)
//        );

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

            System.out.println("All Done");
        } catch (Exception e) {
            System.out.println("SOMETHING WENT WRONG:");
        }
    }

    private CompletableFuture<Void> runCollector(Runnable collectorTask) {
        return CompletableFuture.runAsync(collectorTask, loadDependenciesExecutor);
    }
}
