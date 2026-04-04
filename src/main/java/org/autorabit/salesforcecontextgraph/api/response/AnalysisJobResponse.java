package org.autorabit.salesforcecontextgraph.api.response;

import java.time.LocalDateTime;
import java.util.Map;
import org.autorabit.salesforcecontextgraph.domain.enums.AnalysisStatus;
import org.autorabit.salesforcecontextgraph.domain.enums.AnalysisType;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;

public record AnalysisJobResponse(
        Long jobId,
        String orgId,
        AnalysisType analysisType,
        NodeType targetType,
        String targetName,
        AnalysisStatus status,
        String errorMessage,
        LocalDateTime createdAt,
        Map<String, Object> result
) {
}
