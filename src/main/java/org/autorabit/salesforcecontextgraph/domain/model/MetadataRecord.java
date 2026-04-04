package org.autorabit.salesforcecontextgraph.domain.model;

import java.util.List;

public record MetadataRecord(
        String id,
        String type,
        String name,
        List<MetadataDependency> dependencies
) {
    public record MetadataDependency(String targetId, String relationshipType) {
    }
}
