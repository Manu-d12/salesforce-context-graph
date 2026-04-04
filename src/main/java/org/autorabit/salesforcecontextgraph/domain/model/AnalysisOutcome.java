package org.autorabit.salesforcecontextgraph.domain.model;

import java.util.List;
import java.util.Map;

public record AnalysisOutcome(
        List<String> dependencies,
        List<String> injectionOrder,
        List<String> path,
        String severity,
        Map<String, Object> metadata
) {
}
