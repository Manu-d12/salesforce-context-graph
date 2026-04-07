package org.autorabit.salesforcecontextgraph.domain.model;

public record GraphNode(
        String id,
        String type,
        String name
) {
    public static GraphNode buildGraphNode(String id, String type, String name) {
        String newId = id + " - " + type;
        return new GraphNode(newId, type, name);
    }
}
