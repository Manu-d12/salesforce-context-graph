package org.autorabit.salesforcecontextgraph.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.autorabit.salesforcecontextgraph.domain.model.AnalysisOutcome;
import org.autorabit.salesforcecontextgraph.domain.model.AnalysisRequest;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.domain.model.RuntimeGraph;
import org.springframework.stereotype.Service;

@Service
public class DependencyAgent {

    public AnalysisOutcome analyze(AnalysisRequest request, RuntimeGraph graph) {
        Set<String> visited = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        String startNodeId = resolveStartNodeId(request.targetName(), graph);
        queue.add(startNodeId);

        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            List<GraphEdge> outgoing = graph.edges().getOrDefault(current, List.of());
            for (GraphEdge edge : outgoing) {
                if (isDependencyEdge(edge.type())) {
                    queue.addLast(edge.toNode().id());
                }
            }
        }

        String startNodeName = graph.nodes().getOrDefault(startNodeId, new GraphNode(startNodeId, "UNKNOWN", startNodeId)).name();
        List<String> dependencies = visited.stream()
                .map(nodeId -> graph.nodes().getOrDefault(nodeId, new GraphNode(nodeId, "UNKNOWN", nodeId)).name())
                .filter(nodeName -> !startNodeName.equals(nodeName))
                .sorted(Comparator.naturalOrder())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        return new AnalysisOutcome(
                dependencies,
                new ArrayList<>(dependencies),
                List.of(),
                null,
                java.util.Map.of(
                        "target", request.targetName(),
                        "nodeCount", graph.nodes().size(),
                        "edgeCount", graph.edges().values().stream().mapToInt(List::size).sum()
                )
        );
    }

    private String resolveStartNodeId(String targetName, RuntimeGraph graph) {
        if (graph.nodes().containsKey(targetName)) {
            return targetName;
        }
        return graph.nodes().values().stream()
                .filter(node -> targetName.equals(node.name()))
                .map(GraphNode::id)
                .findFirst()
                .orElse(targetName);
    }

    private boolean isDependencyEdge(String edgeType) {
        return "DEPENDS_ON".equals(edgeType)
                || "USES_FIELD".equals(edgeType)
                || "REQUIRES_OBJECT".equals(edgeType)
                || "REQUIRES_FIELD".equals(edgeType);
    }
}
