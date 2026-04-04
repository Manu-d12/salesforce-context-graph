package org.autorabit.salesforcecontextgraph.api.response;

public record SalesforceSessionResponse(
        String instanceUrl,
        String idUrl,
        String apiVersion
) {
}
