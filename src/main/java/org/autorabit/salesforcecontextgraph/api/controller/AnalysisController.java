package org.autorabit.salesforcecontextgraph.api.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.autorabit.salesforcecontextgraph.api.request.AnalysisExecutionRequestDto;
import org.autorabit.salesforcecontextgraph.api.request.AnalysisRequestDto;
import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.api.response.AnalysisGraphResponse;
import org.autorabit.salesforcecontextgraph.api.response.GraphEdgeResponse;
import org.autorabit.salesforcecontextgraph.api.response.GraphNodeResponse;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.domain.model.RuntimeGraph;
import org.autorabit.salesforcecontextgraph.service.AnalysisOrchestratorAgent;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisOrchestratorAgent orchestratorAgent;

    public AnalysisController(
            AnalysisOrchestratorAgent orchestratorAgent
    ) {
        this.orchestratorAgent = orchestratorAgent;
    }

    @PostMapping
    public AnalysisGraphResponse createAnalysis(@RequestBody SfOrgSyncRequestDto requestDto) {
        RuntimeGraph graph = orchestratorAgent.loadOrganizationGraph(requestDto);
        List<GraphEdgeResponse> edges = flattenEdges(graph.edges());
        return new AnalysisGraphResponse(toNodeResponses(graph.nodes()), edges);
    }

    @PostMapping("/custom-standard-object-relations")
    public AnalysisGraphResponse createCustomStandardObjectRelationsAnalysis(@RequestBody SfOrgSyncRequestDto requestDto) {
        RuntimeGraph graph = orchestratorAgent.buildCustomStandardObjectRelationGraph(requestDto);
        List<GraphEdgeResponse> edges = flattenEdges(graph.edges());
        return new AnalysisGraphResponse(toNodeResponses(graph.nodes()), edges);
    }


    @PostMapping("/permission-set-group-relations")
    public AnalysisGraphResponse createPermissionSetGroupRelationsAnalysis(@RequestBody SfOrgSyncRequestDto requestDto) {
        RuntimeGraph graph = orchestratorAgent.buildPermissionGetGroupRelationGraph(requestDto);
        List<GraphEdgeResponse> edges = flattenEdges(graph.edges());
        return new AnalysisGraphResponse(toNodeResponses(graph.nodes()), edges);

    }

    @PostMapping("/permission-set-relations")
    public AnalysisGraphResponse createPermissionSetRelationsAnalysis(@RequestBody SfOrgSyncRequestDto requestDto) {
        RuntimeGraph graph = orchestratorAgent.buildPermissionSetRelationGraph(requestDto);
        List<GraphEdgeResponse> edges = flattenEdges(graph.edges());
        return new AnalysisGraphResponse(toNodeResponses(graph.nodes()), edges);

    }



    @PostMapping("/target")
    public AnalysisGraphResponse createTargetMetadataAnalysis(@RequestBody AnalysisExecutionRequestDto requestDto) {
        if (requestDto == null || requestDto.analysis() == null) {
            throw new IllegalArgumentException("analysis request is required");
        }
        RuntimeGraph graph = orchestratorAgent.runTargetMetadataAnalysis(requestDto.analysis(), requestDto.salesforce());
        List<GraphEdgeResponse> edges = flattenEdges(graph.edges());
        return new AnalysisGraphResponse(toNodeResponses(graph.nodes()), edges);
    }

    @GetMapping("/message")
    public String getAnalysisMessage() {
        return "Analysis response placeholder";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }

    private List<GraphNodeResponse> toNodeResponses(Map<String, GraphNode> nodes) {
        return nodes.values().stream()
                .map(this::toNodeResponse)
                .toList();
    }

    private List<GraphNodeResponse> toNodeResponses(List<GraphEdge> edges) {
        Map<String, GraphNode> nodes = new LinkedHashMap<>();
        for (GraphEdge edge : edges) {
            nodes.put(edge.fromNode().id(), edge.fromNode());
            nodes.put(edge.toNode().id(), edge.toNode());
        }
        return toNodeResponses(nodes);
    }

    private List<GraphEdgeResponse> flattenEdges(Map<String, List<GraphEdge>> adjacency) {
        List<GraphEdgeResponse> responses = new ArrayList<>();
        for (List<GraphEdge> edges : adjacency.values()) {
            for (GraphEdge edge : edges) {
                responses.add(new GraphEdgeResponse(
                        toNodeResponse(edge.fromNode()),
                        toNodeResponse(edge.toNode()),
                        edge.type()
                ));
            }
        }
        return responses;
    }

    private GraphNodeResponse toNodeResponse(GraphNode node) {
        return new GraphNodeResponse(node.id(), node.type(), node.name());
    }
}
