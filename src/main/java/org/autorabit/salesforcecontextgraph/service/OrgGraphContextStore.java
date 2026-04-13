package org.autorabit.salesforcecontextgraph.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.autorabit.salesforcecontextgraph.api.response.AnalysisGraphResponse;
import org.autorabit.salesforcecontextgraph.api.response.GraphEdgeResponse;
import org.autorabit.salesforcecontextgraph.api.response.GraphNodeResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class OrgGraphContextStore {

    private static final int MAX_NODES_IN_CONTEXT = 120;
    private static final int MAX_EDGES_IN_CONTEXT = 180;

    private final ConcurrentMap<String, GraphSnapshot> graphByOrgId = new ConcurrentHashMap<>();
    private final AtomicLong versionCounter = new AtomicLong(0L);
    private final ObjectMapper objectMapper;

    public OrgGraphContextStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void upsert(String sfOrgId, AnalysisGraphResponse graphResponse) {
        if (sfOrgId == null || sfOrgId.isBlank() || graphResponse == null) {
            return;
        }
        String normalizedOrgId = sfOrgId.trim();
        long nextVersion = versionCounter.incrementAndGet();
        String contextJson = buildContextJson(normalizedOrgId, graphResponse);
        graphByOrgId.put(normalizedOrgId, new GraphSnapshot(graphResponse, nextVersion, contextJson));
    }

    public Optional<AnalysisGraphResponse> findByOrgId(String sfOrgId) {
        if (sfOrgId == null || sfOrgId.isBlank()) {
            return Optional.empty();
        }
        GraphSnapshot snapshot = graphByOrgId.get(sfOrgId.trim());
        return snapshot == null ? Optional.empty() : Optional.of(snapshot.graphResponse());
    }

    public Optional<GraphContextPayload> findGraphContext(String sfOrgId) {
        if (sfOrgId == null || sfOrgId.isBlank()) {
            return Optional.empty();
        }
        GraphSnapshot snapshot = graphByOrgId.get(sfOrgId.trim());
        if (snapshot == null) {
            return Optional.empty();
        }
        return Optional.of(new GraphContextPayload(snapshot.version(), snapshot.contextJson()));
    }

    private String buildContextJson(String sfOrgId, AnalysisGraphResponse graph) {
        List<GraphNodeResponse> nodes = graph.nodes() == null ? List.of() : graph.nodes();
        List<GraphEdgeResponse> edges = graph.edges() == null ? List.of() : graph.edges();

        List<GraphNodeResponse> limitedNodes = nodes.size() > MAX_NODES_IN_CONTEXT
                ? nodes.subList(0, MAX_NODES_IN_CONTEXT)
                : nodes;
        List<GraphEdgeResponse> limitedEdges = edges.size() > MAX_EDGES_IN_CONTEXT
                ? edges.subList(0, MAX_EDGES_IN_CONTEXT)
                : edges;

        LinkedHashMap<String, Object> context = new LinkedHashMap<>();
        context.put("orgId", sfOrgId);
        context.put("totalNodes", nodes.size());
        context.put("totalEdges", edges.size());
        context.put("sampledNodes", limitedNodes);
        context.put("sampledEdges", limitedEdges);

        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            return "{\"orgId\":\"%s\",\"error\":\"failed_to_serialize_graph_context\"}".formatted(sfOrgId);
        }
    }

    private record GraphSnapshot(
            AnalysisGraphResponse graphResponse,
            long version,
            String contextJson
    ) {
    }

    public record GraphContextPayload(
            long version,
            String contextJson
    ) {
    }
}
