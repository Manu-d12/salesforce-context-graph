package org.autorabit.salesforcecontextgraph.api.request;

import org.autorabit.salesforcecontextgraph.domain.enums.AnalysisType;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;

import java.util.List;
import java.util.Map;

public record AnalysisRequestDto (
        AnalysisType analysisType,
        Map<NodeType, List<String>> targetNodes
) {
}
