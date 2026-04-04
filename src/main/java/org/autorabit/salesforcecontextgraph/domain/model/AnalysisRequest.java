package org.autorabit.salesforcecontextgraph.domain.model;

import org.autorabit.salesforcecontextgraph.domain.enums.AnalysisType;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;

public record AnalysisRequest(
        AnalysisType analysisType,
        NodeType targetType,
        String targetName
) {
}
