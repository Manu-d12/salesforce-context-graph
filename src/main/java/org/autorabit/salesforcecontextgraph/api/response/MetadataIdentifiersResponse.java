package org.autorabit.salesforcecontextgraph.api.response;

import java.util.List;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;

public record MetadataIdentifiersResponse(
        List<MetadataApiClient.MetadataIdentifier> objects
) {
}
