package org.autorabit.salesforcecontextgraph.utils;

import org.autorabit.salesforcecontextgraph.db_entities.MetadataDependency;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;

public class Helper {

    public static MetadataDependency buildMetadataDependency(GraphEdge edge) {
        return buildMetadataDependency(edge, null);
    }

    public static MetadataDependency buildMetadataDependency(GraphEdge edge, String orgId) {
        return MetadataDependency.builder()
                .orgId(orgId)
                .metadataLabel(edge.fromNode().name())
                .metadataName(edge.fromNode().id())
                .metadataType(edge.fromNode().type())
                .refMetadataLabel(edge.toNode().name())
                .refMetadataName(edge.toNode().id())
                .refMetadataType(edge.toNode().type())
                .edgeType(edge.type())
                .build();
    }

    public static String resolveOrgId(MetadataApiClient metadataApiClient) {
        return metadataApiClient.resolveOrgId();
    }

    public static String resolveOrgId(MetadataApiClient metadataApiClient, SalesforceSession session) {
        return metadataApiClient.resolveOrgId(session);
    }
}
