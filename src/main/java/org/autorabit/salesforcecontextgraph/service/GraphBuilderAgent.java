package org.autorabit.salesforcecontextgraph.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.domain.model.RuntimeGraph;
import org.springframework.stereotype.Service;

@Service
public class GraphBuilderAgent {

    public RuntimeGraph build(List<GraphNode> nodes, List<GraphEdge> edges) {
        Map<String, GraphNode> deduplicatedNodes = new LinkedHashMap<>();
        for (GraphNode node : nodes) {
            deduplicatedNodes.put(node.id(), node);
        }
        for (GraphEdge edge : edges) {
            deduplicatedNodes.put(edge.fromNode().id(), edge.fromNode());
            deduplicatedNodes.put(edge.toNode().id(), edge.toNode());
        }

        Map<String, List<GraphEdge>> adjacency = new LinkedHashMap<>();
        for (GraphEdge edge : edges) {
            adjacency.computeIfAbsent(edge.fromNode().id(), ignored -> new ArrayList<>());
            List<GraphEdge> currentEdges = adjacency.get(edge.fromNode().id());
            boolean exists = currentEdges.stream()
                    .anyMatch(existing -> existing.toNode().id().equals(edge.toNode().id())
                            && existing.type().equals(edge.type()));
            if (!exists) {
                currentEdges.add(edge);
            }
        }

        return new RuntimeGraph(deduplicatedNodes, adjacency);
    }

    public RuntimeGraph build(List<GraphEdge> edges) {
        return build(List.of(), edges);
    }
}
