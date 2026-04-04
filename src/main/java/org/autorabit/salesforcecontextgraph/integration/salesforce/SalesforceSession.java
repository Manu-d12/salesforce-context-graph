package org.autorabit.salesforcecontextgraph.integration.salesforce;

public record SalesforceSession(
        String accessToken,
        String instanceUrl,
        String idUrl
) {
}
