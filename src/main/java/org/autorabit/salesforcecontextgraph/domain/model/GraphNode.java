package org.autorabit.salesforcecontextgraph.domain.model;

public record GraphNode(
        String id,
        String type,
        String name
) {
    public static GraphNode buildGraphNode(String name, String type) {
        String newId = name + "$$$" + type;
        return new GraphNode(newId, type, name);
    }
}
