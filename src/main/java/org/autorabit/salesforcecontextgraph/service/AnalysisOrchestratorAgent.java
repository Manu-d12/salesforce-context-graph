package org.autorabit.salesforcecontextgraph.service;

import org.autorabit.salesforcecontextgraph.api.request.AnalysisRequestDto;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomStandardObjectDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.MetadataComponentDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetGroupDependenciesCollector;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.RuntimeGraph;
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

    public AnalysisOrchestratorAgent(
            MetadataComponentDependencyCollector metadataComponentDependencyCollector,
            GraphBuilderAgent graphBuilderAgent,
            CustomStandardObjectDependencyCollector customStandardObjectDependencyCollector,
            PermissionSetDependenciesCollector permissionSetDependenciesCollector,
            PermissionSetGroupDependenciesCollector permissionSetGroupDependenciesCollector,
            TargetDependencyGraphBuilder targetDependencyGraphBuilder
    ) {
        this.metadataComponentDependencyCollector = metadataComponentDependencyCollector;
        this.graphBuilderAgent = graphBuilderAgent;
        this.customStandardObjectDependencyCollector = customStandardObjectDependencyCollector;
        this.permissionSetDependenciesCollector = permissionSetDependenciesCollector;
        this.permissionSetGroupDependenciesCollector = permissionSetGroupDependenciesCollector;
        this.targetDependencyGraphBuilder = targetDependencyGraphBuilder;
    }

    public RuntimeGraph loadOrganizationGraph() {
        List<GraphEdge> permissionSetDependencies = permissionSetDependenciesCollector.buildRelativeGraphEdges();
        List<GraphEdge> objectRelations = customStandardObjectDependencyCollector.buildRelativeGraphEdges();
        List<GraphEdge> metadataEdges = metadataComponentDependencyCollector.buildRelativeGraphEdges();
        List<GraphEdge> permissionSetGroupRelations = permissionSetGroupDependenciesCollector.buildRelativeGraphEdges();

        List<GraphEdge> organizationGraphEdges = new ArrayList<>();
        organizationGraphEdges.addAll(permissionSetDependencies);
        organizationGraphEdges.addAll(objectRelations);
        organizationGraphEdges.addAll(metadataEdges);
        organizationGraphEdges.addAll(permissionSetGroupRelations);

        return graphBuilderAgent.build(
                organizationGraphEdges
        );
    }

    public RuntimeGraph buildPermissionSetRelationGraph() {
        List<GraphEdge> edges = permissionSetDependenciesCollector.buildRelativeGraphEdges();
        return graphBuilderAgent.build(
                edges
        );
    }

    public RuntimeGraph buildCustomStandardObjectRelationGraph() {
        List<GraphEdge> edges = customStandardObjectDependencyCollector.buildRelativeGraphEdges();
        return graphBuilderAgent.build(
                edges
        );
    }

    public RuntimeGraph buildPermissionGetGroupRelationGraph() {
        List<GraphEdge> edges = permissionSetGroupDependenciesCollector.buildRelativeGraphEdges();
        return graphBuilderAgent.build(
                edges
        );
    }

    public RuntimeGraph runTargetMetadataAnalysis(AnalysisRequestDto request) {
        return targetDependencyGraphBuilder.buildGraph(request);
    }
}
