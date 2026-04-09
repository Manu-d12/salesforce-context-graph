package org.autorabit.salesforcecontextgraph.service;

import org.autorabit.salesforcecontextgraph.api.request.AnalysisRequestDto;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomStandardObjectDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.MetadataComponentDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetGroupDependenciesCollector;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.RuntimeGraph;
import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceOAuthService;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisOrchestratorAgent {

    private final MetadataComponentDependencyCollector metadataComponentDependencyCollector;
    private final GraphBuilderAgent graphBuilderAgent;
    private final CustomStandardObjectDependencyCollector customStandardObjectDependencyCollector;
    private final PermissionSetDependenciesCollector permissionSetDependenciesCollector;
    private final PermissionSetGroupDependenciesCollector permissionSetGroupDependenciesCollector;
    private final TargetDependencyGraphBuilder targetDependencyGraphBuilder;
    private final SalesforceOAuthService salesforceOAuthService;

    public AnalysisOrchestratorAgent(
            MetadataComponentDependencyCollector metadataComponentDependencyCollector,
            GraphBuilderAgent graphBuilderAgent,
            CustomStandardObjectDependencyCollector customStandardObjectDependencyCollector,
            PermissionSetDependenciesCollector permissionSetDependenciesCollector,
            PermissionSetGroupDependenciesCollector permissionSetGroupDependenciesCollector,
            TargetDependencyGraphBuilder targetDependencyGraphBuilder,
            SalesforceOAuthService salesforceOAuthService
    ) {
        this.metadataComponentDependencyCollector = metadataComponentDependencyCollector;
        this.graphBuilderAgent = graphBuilderAgent;
        this.customStandardObjectDependencyCollector = customStandardObjectDependencyCollector;
        this.permissionSetDependenciesCollector = permissionSetDependenciesCollector;
        this.permissionSetGroupDependenciesCollector = permissionSetGroupDependenciesCollector;
        this.targetDependencyGraphBuilder = targetDependencyGraphBuilder;
        this.salesforceOAuthService = salesforceOAuthService;
    }

    public RuntimeGraph loadOrganizationGraph(SfOrgSyncRequestDto requestDto) {
        SalesforceSession session = resolveSession(requestDto);
        List<GraphEdge> permissionSetDependencies = permissionSetDependenciesCollector.buildRelativeGraphEdges(session);
        List<GraphEdge> objectRelations = customStandardObjectDependencyCollector.buildRelativeGraphEdges(session);
        List<GraphEdge> metadataEdges = metadataComponentDependencyCollector.buildRelativeGraphEdges(session);
        List<GraphEdge> permissionSetGroupRelations = permissionSetGroupDependenciesCollector.buildRelativeGraphEdges(session);

        List<GraphEdge> organizationGraphEdges = new ArrayList<>();
        organizationGraphEdges.addAll(permissionSetDependencies);
        organizationGraphEdges.addAll(objectRelations);
        organizationGraphEdges.addAll(metadataEdges);
        organizationGraphEdges.addAll(permissionSetGroupRelations);

        return graphBuilderAgent.build(
                organizationGraphEdges
        );
    }

    public RuntimeGraph buildPermissionSetRelationGraph(SfOrgSyncRequestDto requestDto) {
        List<GraphEdge> edges = permissionSetDependenciesCollector.buildRelativeGraphEdges(resolveSession(requestDto));
        return graphBuilderAgent.build(
                edges
        );
    }

    public RuntimeGraph buildCustomStandardObjectRelationGraph(SfOrgSyncRequestDto requestDto) {
        List<GraphEdge> edges = customStandardObjectDependencyCollector.buildRelativeGraphEdges(resolveSession(requestDto));
        return graphBuilderAgent.build(
                edges
        );
    }

    public RuntimeGraph buildPermissionGetGroupRelationGraph(SfOrgSyncRequestDto requestDto) {
        List<GraphEdge> edges = permissionSetGroupDependenciesCollector.buildRelativeGraphEdges(resolveSession(requestDto));
        return graphBuilderAgent.build(
                edges
        );
    }

    public RuntimeGraph runTargetMetadataAnalysis(AnalysisRequestDto request, SfOrgSyncRequestDto authRequest) {
        return targetDependencyGraphBuilder.buildGraph(request, resolveSession(authRequest));
    }

    private SalesforceSession resolveSession(SfOrgSyncRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }
        if (isBlank(requestDto.loginUrl()) || isBlank(requestDto.clientId()) || isBlank(requestDto.clientSecret())) {
            return null;
        }
        return salesforceOAuthService.authenticate(requestDto);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
