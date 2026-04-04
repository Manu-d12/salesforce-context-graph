package org.autorabit.salesforcecontextgraph.service;

import org.autorabit.salesforcecontextgraph.domain.model.AnalysisRequest;
import org.springframework.stereotype.Service;

@Service
public class RequestValidationAgent {

    public AnalysisRequest validate(AnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        return new AnalysisRequest(
                request.analysisType(),
                request.targetType(),
                request.targetName() == null ? null : request.targetName().trim()
        );
    }
}
