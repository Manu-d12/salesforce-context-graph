package org.autorabit.salesforcecontextgraph.domain.model;

import org.autorabit.salesforcecontextgraph.domain.enums.DependencyStrength;
import org.autorabit.salesforcecontextgraph.domain.enums.EdgeType;
import org.jspecify.annotations.NonNull;

public record EdgeResolution(
        EdgeType edgeType,
        DependencyStrength dependencyStrength
) {

    @Override
    public @NonNull String toString() {
        return this.edgeType.toString() + " - " + this.dependencyStrength.toString();
    }
}