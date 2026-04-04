package org.autorabit.salesforcecontextgraph.domain.model;

public record GraphEdge(
        GraphNode fromNode,
        GraphNode toNode,
        String type
) {
}
