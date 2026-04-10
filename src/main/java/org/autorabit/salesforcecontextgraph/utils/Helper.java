package org.autorabit.salesforcecontextgraph.utils;

import org.autorabit.salesforcecontextgraph.db_entities.MetadataDependency;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;

public class Helper {

    public static MetadataDependency buildMetadataDependency(GraphEdge edge, String orgId, String edgeSource) {
        return MetadataDependency.builder()
                .orgId(orgId)
                .metadataLabel(edge.fromNode().name())
                .metadataName(edge.fromNode().name())
                .metadataType(edge.fromNode().type())
                .refMetadataLabel(edge.toNode().name())
                .refMetadataName(edge.toNode().name())
                .refMetadataType(edge.toNode().type())
                .edgeType(edge.type())
                .edgeSource(edgeSource)
                .build();
    }

    public static String resolveOrgId(MetadataApiClient metadataApiClient, SalesforceSession session) {
        return metadataApiClient.resolveOrgId(session);
    }
}
