package org.autorabit.salesforcecontextgraph.domain.model;

import java.util.List;
import java.util.Map;

public record GraphResult(
        String rootNodeId,
        List<GraphNode> nodes,
        List<GraphEdge> edges,
        Map<String, Object> metadata
) {
}
