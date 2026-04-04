package org.autorabit.salesforcecontextgraph.api.response;

import java.util.List;

public record AnalysisGraphResponse(
        List<GraphNodeResponse> nodes,
        List<GraphEdgeResponse> edges
) {
}
