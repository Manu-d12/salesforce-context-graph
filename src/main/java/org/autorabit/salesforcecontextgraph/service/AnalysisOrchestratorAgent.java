package org.autorabit.salesforcecontextgraph.service;

import org.autorabit.salesforcecontextgraph.api.request.AnalysisRequestDto;
import org.autorabit.salesforcecontextgraph.domain.model.AnalysisRequest;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.domain.model.RuntimeGraph;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceFetchAgent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AnalysisOrchestratorAgent {

    private final RequestValidationAgent requestValidationAgent;
    private final SalesforceFetchAgent salesforceFetchAgent;
    private final GraphBuilderAgent graphBuilderAgent;
    private final CustomStandardObjectEdgeBuilder customStandardObjectEdgeBuilder;


    public AnalysisOrchestratorAgent(
            RequestValidationAgent requestValidationAgent,
            SalesforceFetchAgent salesforceFetchAgent,
            GraphBuilderAgent graphBuilderAgent,
            CustomStandardObjectEdgeBuilder customStandardObjectEdgeBuilder
    ) {
        this.requestValidationAgent = requestValidationAgent;
        this.salesforceFetchAgent = salesforceFetchAgent;
        this.graphBuilderAgent = graphBuilderAgent;
        this.customStandardObjectEdgeBuilder = customStandardObjectEdgeBuilder;
    }

    @Transactional
    public RuntimeGraph runAnalysis(AnalysisRequest request) {
        AnalysisRequest validatedRequest = requestValidationAgent.validate(request);
        java.util.List<GraphEdge> edges = salesforceFetchAgent.fetchMetadata(validatedRequest);
        return graphBuilderAgent.build(edges);
    }

    @Transactional(readOnly = true)
    public RuntimeGraph loadDependencyGraph() {
        List<GraphEdge> objectRelations = runCustomStandardObjectRelationAnalysis();
        List<GraphEdge> metadataEdges = salesforceFetchAgent.fetchMetadata(new AnalysisRequest(null, null, null));
        metadataEdges.addAll(objectRelations);
        return graphBuilderAgent.build(
                metadataEdges
        );
    }

    public List<GraphEdge> runCustomStandardObjectRelationAnalysis() {
        List<GraphEdge> edges = customStandardObjectEdgeBuilder.buildGraphEdges();
        return edges;
    }

    public List<GraphEdge> runTargetMetadataAnalysis(AnalysisRequestDto request) {
        RuntimeGraph fullGraph = graphBuilderAgent.build(
                salesforceFetchAgent.fetchMetadata(new AnalysisRequest(request.analysisType(), request.targetType(), request.targetName()))
        );
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
        return targetEdges;
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
