package org.autorabit.salesforcecontextgraph.api.request;

import java.util.List;

public record MetadataDescribeRequestDto(
        String metadataType,
        List<String> metadataApiNames
) {
}
