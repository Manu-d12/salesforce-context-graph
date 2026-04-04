package org.autorabit.salesforcecontextgraph.domain.model;

import java.util.List;
import java.util.Map;

public record RuntimeGraph(
        Map<String, GraphNode> nodes,
        Map<String, List<GraphEdge>> edges
) {
}
