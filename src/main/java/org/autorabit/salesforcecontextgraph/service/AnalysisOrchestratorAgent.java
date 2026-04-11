package org.autorabit.salesforcecontextgraph.service;

import org.autorabit.salesforcecontextgraph.api.request.AnalysisRequestDto;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomApplicationDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomStandardObjectDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomTabDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.MetadataComponentDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetGroupDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.ProfileDependenciesCollector;
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
    private final CustomApplicationDependenciesCollector customApplicationDependenciesCollector;
    private final CustomStandardObjectDependencyCollector customStandardObjectDependencyCollector;
    private final CustomTabDependenciesCollector customTabDependenciesCollector;
    private final PermissionSetDependenciesCollector permissionSetDependenciesCollector;
    private final PermissionSetGroupDependenciesCollector permissionSetGroupDependenciesCollector;
    private final ProfileDependenciesCollector profileDependenciesCollector;
    private final TargetDependencyGraphBuilder targetDependencyGraphBuilder;
    private final SalesforceOAuthService salesforceOAuthService;

    public AnalysisOrchestratorAgent(
            MetadataComponentDependencyCollector metadataComponentDependencyCollector,
            GraphBuilderAgent graphBuilderAgent,
            CustomApplicationDependenciesCollector customApplicationDependenciesCollector,
            CustomStandardObjectDependencyCollector customStandardObjectDependencyCollector,
            CustomTabDependenciesCollector customTabDependenciesCollector,
            PermissionSetDependenciesCollector permissionSetDependenciesCollector,
            PermissionSetGroupDependenciesCollector permissionSetGroupDependenciesCollector,
            ProfileDependenciesCollector profileDependenciesCollector,
            TargetDependencyGraphBuilder targetDependencyGraphBuilder,
            SalesforceOAuthService salesforceOAuthService
    ) {
        this.metadataComponentDependencyCollector = metadataComponentDependencyCollector;
        this.graphBuilderAgent = graphBuilderAgent;
        this.customApplicationDependenciesCollector = customApplicationDependenciesCollector;
        this.customStandardObjectDependencyCollector = customStandardObjectDependencyCollector;
        this.customTabDependenciesCollector = customTabDependenciesCollector;
        this.permissionSetDependenciesCollector = permissionSetDependenciesCollector;
        this.permissionSetGroupDependenciesCollector = permissionSetGroupDependenciesCollector;
        this.profileDependenciesCollector = profileDependenciesCollector;
        this.targetDependencyGraphBuilder = targetDependencyGraphBuilder;
        this.salesforceOAuthService = salesforceOAuthService;
    }

    public RuntimeGraph loadOrganizationGraph(SfOrgSyncRequestDto requestDto) {
        SalesforceSession session = resolveSession(requestDto);
        List<GraphEdge> permissionSetDependencies = permissionSetDependenciesCollector.buildRelativeGraphEdges(session);
        List<GraphEdge> objectRelations = customStandardObjectDependencyCollector.buildRelativeGraphEdges(session);
        List<GraphEdge> customApplicationRelations = customApplicationDependenciesCollector.buildRelativeGraphEdges(session);
        List<GraphEdge> customTabRelations = customTabDependenciesCollector.buildRelativeGraphEdges(session);
        List<GraphEdge> metadataEdges = metadataComponentDependencyCollector.buildRelativeGraphEdges(session);
        List<GraphEdge> permissionSetGroupRelations = permissionSetGroupDependenciesCollector.buildRelativeGraphEdges(session);
        List<GraphEdge> profileRelations = profileDependenciesCollector.buildRelativeGraphEdges(session);

        List<GraphEdge> organizationGraphEdges = new ArrayList<>();
        organizationGraphEdges.addAll(permissionSetDependencies);
        organizationGraphEdges.addAll(objectRelations);
        organizationGraphEdges.addAll(customApplicationRelations);
        organizationGraphEdges.addAll(customTabRelations);
        organizationGraphEdges.addAll(metadataEdges);
        organizationGraphEdges.addAll(permissionSetGroupRelations);
        organizationGraphEdges.addAll(profileRelations);

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

    public RuntimeGraph runTargetMetadataAnalysis(AnalysisRequestDto request) {
        return targetDependencyGraphBuilder.buildGraph(request, request.sfOrgId());
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
