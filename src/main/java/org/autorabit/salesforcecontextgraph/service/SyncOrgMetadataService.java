package org.autorabit.salesforcecontextgraph.service;

import lombok.AllArgsConstructor;
import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomStandardObjectDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.MetadataComponentDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetGroupDependenciesCollector;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SyncOrgMetadataService {

    private final CustomStandardObjectDependencyCollector customStandardObjectDependencyCollector;
    private final MetadataComponentDependencyCollector metadataComponentDependencyCollector;
    private PermissionSetDependenciesCollector permissionSetDependenciesCollector;
    private PermissionSetGroupDependenciesCollector permissionSetGroupDependenciesCollector;

    @Async("loadDependenciesExecutor")
    public void sync(SfOrgSyncRequestDto requestDto) {
        metadataComponentDependencyCollector.persistRelativeGraphEdges(requestDto);
        customStandardObjectDependencyCollector.persistRelativeGraphEdges(requestDto);
        permissionSetDependenciesCollector.persistRelativeGraphEdges(requestDto);
        permissionSetGroupDependenciesCollector.persistRelativeGraphEdges(requestDto);
    }



}
