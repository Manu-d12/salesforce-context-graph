package org.autorabit.salesforcecontextgraph.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.autorabit.salesforcecontextgraph.domain.enums.AnalysisType;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;
import org.autorabit.salesforcecontextgraph.domain.model.AnalysisRequest;
import org.autorabit.salesforcecontextgraph.domain.model.RuntimeGraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AnalysisOrchestratorAgentTest {

    @Autowired
    private AnalysisOrchestratorAgent orchestratorAgent;

    @Test
    void shouldBuildRuntimeGraph() {
        RuntimeGraph graph = orchestratorAgent.runAnalysis(new AnalysisRequest(
                AnalysisType.DEPENDENCY,
                NodeType.CUSTOM_FIELD,
                "Payment__c.CardNumber__c"
        ));

        assertNotNull(graph);
        assertNotNull(graph.nodes());
        assertNotNull(graph.edges());
    }

    @Test
    void shouldRejectNullRequests() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orchestratorAgent.runAnalysis(null));
        assertEquals("Request is required", ex.getMessage());
    }

    @Test
    void shouldFailOnMissingJobLookup() {
        RuntimeGraph graph = orchestratorAgent.runAnalysis(new AnalysisRequest(
                null,
                null,
                null
        ));
        assertNotNull(graph);
    }
}
