package org.autorabit.salesforcecontextgraph.service;

import org.autorabit.salesforcecontextgraph.api.request.AnalysisRequestDto;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomStandardObjectDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.MetadataComponentDependencyCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetDependenciesCollector;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.PermissionSetGroupDependenciesCollector;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.domain.model.RuntimeGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AnalysisOrchestratorAgent {

    private final MetadataComponentDependencyCollector metadataComponentDependencyCollector;
    private final GraphBuilderAgent graphBuilderAgent;
    private final CustomStandardObjectDependencyCollector customStandardObjectDependencyCollector;
    private final PermissionSetDependenciesCollector permissionSetDependenciesCollector;
    private final PermissionSetGroupDependenciesCollector permissionSetGroupDependenciesCollector;

    public AnalysisOrchestratorAgent(
            RequestValidationAgent requestValidationAgent,
            MetadataComponentDependencyCollector metadataComponentDependencyCollector,
            GraphBuilderAgent graphBuilderAgent,
            CustomStandardObjectDependencyCollector customStandardObjectDependencyCollector,
            PermissionSetDependenciesCollector permissionSetDependenciesCollector,
            PermissionSetGroupDependenciesCollector permissionSetGroupDependenciesCollector
    ) {
        this.metadataComponentDependencyCollector = metadataComponentDependencyCollector;
        this.graphBuilderAgent = graphBuilderAgent;
        this.customStandardObjectDependencyCollector = customStandardObjectDependencyCollector;
        this.permissionSetDependenciesCollector = permissionSetDependenciesCollector;
        this.permissionSetGroupDependenciesCollector = permissionSetGroupDependenciesCollector;
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
        RuntimeGraph fullGraph = loadOrganizationGraph();
        GraphNode startingNode = null;
        for (String nodeKey : fullGraph.nodes().keySet()) {
            GraphNode node = fullGraph.nodes().get(nodeKey);
            if (node.name().equals(request.targetName()) && node.type().equals(request.targetType().toString())) {
                startingNode = node;
                break;
            }
        }

        if (startingNode == null) {
            throw new IllegalArgumentException("Target metadata not found: " + request.targetName());
        }

        List<GraphEdge> targetEdges = new ArrayList<>();
        Set<String> visitedNodeId = new HashSet<>();
        collectTargetEdges(startingNode, fullGraph, targetEdges, visitedNodeId);
        return graphBuilderAgent.build(targetEdges);
    }

    private void collectTargetEdges(GraphNode node, RuntimeGraph graph, List<GraphEdge> targetEdges, Set<String> visitedNodeId) {
        visitedNodeId.add(node.id());
        for (GraphEdge edge : graph.edges().getOrDefault(node.id(), List.of())) {
            targetEdges.add(edge);
            String nbrNodeId = edge.toNode().id();
            if (!visitedNodeId.contains(nbrNodeId)) {
                collectTargetEdges(graph.nodes().get(nbrNodeId), graph, targetEdges, visitedNodeId);
            }
        }
    }
}
