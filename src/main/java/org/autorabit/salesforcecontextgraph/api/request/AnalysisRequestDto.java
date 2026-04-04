package org.autorabit.salesforcecontextgraph.api.request;

import org.autorabit.salesforcecontextgraph.domain.enums.AnalysisType;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;

public record AnalysisRequestDto(
        AnalysisType analysisType,
        NodeType targetType,
        String targetName
) {
}
