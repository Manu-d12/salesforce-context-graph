package org.autorabit.salesforcecontextgraph.service;

import lombok.AllArgsConstructor;
import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomApplicationDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomStandardObjectDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomTabDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.MetadataComponentDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetGroupDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.ProfileDependenciesCollector;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceOAuthService;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SyncOrgMetadataService {

    private final CustomApplicationDependenciesCollector customApplicationDependenciesCollector;
    private final CustomStandardObjectDependencyCollector customStandardObjectDependencyCollector;
    private final CustomTabDependenciesCollector customTabDependenciesCollector;
    private final MetadataComponentDependencyCollector metadataComponentDependencyCollector;
    private final SalesforceOAuthService salesforceOAuthService;
    private PermissionSetDependenciesCollector permissionSetDependenciesCollector;
    private PermissionSetGroupDependenciesCollector permissionSetGroupDependenciesCollector;
    private ProfileDependenciesCollector profileDependenciesCollector;

    @Async("loadDependenciesExecutor")
    public void sync(SfOrgSyncRequestDto requestDto) {
        SalesforceSession session = salesforceOAuthService.authenticate(requestDto);
        metadataComponentDependencyCollector.persistRelativeGraphEdges(requestDto, session);
        customStandardObjectDependencyCollector.persistRelativeGraphEdges(requestDto, session);
        customApplicationDependenciesCollector.persistRelativeGraphEdges(requestDto, session);
        customTabDependenciesCollector.persistRelativeGraphEdges(requestDto, session);
        permissionSetDependenciesCollector.persistRelativeGraphEdges(requestDto, session);
        permissionSetGroupDependenciesCollector.persistRelativeGraphEdges(requestDto, session);
//        profileDependenciesCollector.persistRelativeGraphEdges(requestDto, session);
    }
}
