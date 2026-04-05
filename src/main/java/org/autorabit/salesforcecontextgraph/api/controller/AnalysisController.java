package org.autorabit.salesforcecontextgraph.api.controller;

import com.sforce.soap.metadata.DescribeMetadataResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.autorabit.salesforcecontextgraph.api.request.AnalysisRequestDto;
import org.autorabit.salesforcecontextgraph.api.request.FieldDefinitionsRequestDto;
import org.autorabit.salesforcecontextgraph.api.response.AnalysisGraphResponse;
import org.autorabit.salesforcecontextgraph.api.response.GraphEdgeResponse;
import org.autorabit.salesforcecontextgraph.api.response.GraphNodeResponse;
import org.autorabit.salesforcecontextgraph.api.response.MetadataObjectsResponse;
import org.autorabit.salesforcecontextgraph.collectors.CustomStandardObjectRelationsCollector;
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
    private final CustomStandardObjectRelationsCollector collector;

    public AnalysisController(
            AnalysisOrchestratorAgent orchestratorAgent,
            CustomStandardObjectRelationsCollector collector
    ) {
        this.orchestratorAgent = orchestratorAgent;
        this.collector = collector;
    }

    @PostMapping
    public AnalysisGraphResponse createAnalysis() {
        RuntimeGraph graph = orchestratorAgent.loadDependencyGraph();
        List<GraphEdgeResponse> edges = flattenEdges(graph.edges());
        return new AnalysisGraphResponse(toNodeResponses(graph.nodes()), edges);
    }

    @PostMapping("/target")
    public AnalysisGraphResponse createTargetMetadataAnalysis(@RequestBody AnalysisRequestDto requestDto) {
        List<GraphEdge> graphEdges = orchestratorAgent.runTargetMetadataAnalysis(requestDto);
        List<GraphEdgeResponse> edges = graphEdges.stream()
                .map(edge -> new GraphEdgeResponse(
                        toNodeResponse(edge.fromNode()),
                        toNodeResponse(edge.toNode()),
                        edge.type()
                ))
                .toList();
        return new AnalysisGraphResponse(toNodeResponses(graphEdges), edges);
    }

    @GetMapping("/message")
    public String getAnalysisMessage() {
        return "Analysis response placeholder";
    }

    @GetMapping("/metadata/{metadataType}")
    public MetadataObjectsResponse listMetadataObjects(
            @PathVariable String metadataType
    ) {
        return new MetadataObjectsResponse(collector.listMetadataFullNames(metadataType));
    }

    @GetMapping("/metadata/describe")
    public DescribeMetadataResult describeMetadata() {
        return collector.describeMetadata();
    }

    @PostMapping("/metadata/field-definitions")
    public List<Map<String, Object>> getFieldDefinitions(@RequestBody FieldDefinitionsRequestDto requestDto) {
        if (requestDto == null || requestDto.fieldApiNames() == null || requestDto.fieldApiNames().isEmpty()) {
            throw new IllegalArgumentException("fieldApiNames is required");
        }
        return collector.getFieldDefinitions(requestDto.fieldApiNames());
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
