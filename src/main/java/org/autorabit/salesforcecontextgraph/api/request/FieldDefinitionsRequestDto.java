package org.autorabit.salesforcecontextgraph.api.request;

import java.util.List;

public record FieldDefinitionsRequestDto(
        List<String> fieldApiNames
) {
}
