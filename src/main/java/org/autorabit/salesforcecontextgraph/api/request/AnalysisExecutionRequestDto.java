package org.autorabit.salesforcecontextgraph.api.request;

public record AnalysisExecutionRequestDto(
        SfOrgSyncRequestDto salesforce,
        AnalysisRequestDto analysis
) {
}
