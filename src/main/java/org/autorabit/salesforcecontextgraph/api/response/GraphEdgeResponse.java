package org.autorabit.salesforcecontextgraph.api.response;

public record GraphEdgeResponse(
        GraphNodeResponse fromNode,
        GraphNodeResponse toNode,
        String type
) {
}
