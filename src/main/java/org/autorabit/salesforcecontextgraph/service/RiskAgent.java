package org.autorabit.salesforcecontextgraph.service;

import org.autorabit.salesforcecontextgraph.domain.model.AnalysisOutcome;
import org.springframework.stereotype.Service;

@Service
public class RiskAgent {

    public String score(AnalysisOutcome outcome) {
        if (outcome.path() != null && outcome.path().size() >= 4) {
            return "HIGH";
        }
        if (outcome.path() != null && !outcome.path().isEmpty()) {
            return "MEDIUM";
        }
        if (outcome.dependencies() != null && !outcome.dependencies().isEmpty()) {
            return "LOW";
        }
        return "LOW";
    }
}
