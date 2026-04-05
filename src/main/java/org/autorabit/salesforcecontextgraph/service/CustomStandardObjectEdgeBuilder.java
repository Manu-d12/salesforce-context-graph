package org.autorabit.salesforcecontextgraph.service;

import lombok.AllArgsConstructor;
import org.autorabit.salesforcecontextgraph.collectors.CustomStandardObjectRelationsCollector;
import org.autorabit.salesforcecontextgraph.domain.enums.EdgeType;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class CustomStandardObjectEdgeBuilder {

    private final CustomStandardObjectRelationsCollector collector;

    public List<GraphEdge> buildGraphEdges() {
        List<String> customFields = collector.listMetadataFullNames("CustomField");
        List<Map<String, Object>> fieldDefinitions = collector.getFieldDefinitions(customFields);

        List<GraphEdge> edges = new ArrayList<>();
        for (Map<String, Object> fieldDefinition : fieldDefinitions) {
            String objectApiName = extractEntityApiName(fieldDefinition.get("EntityDefinition"));
            if (objectApiName == null) {
                continue;
            }

            String referencedObject = extractFirstReferenceTarget(fieldDefinition);
            if (referencedObject == null) {
                continue;
            }

            GraphNode fromNode = new GraphNode(
                    objectApiName,
                    resolveObjectType(objectApiName).toString(),
                    objectApiName
            );
            GraphNode toNode = new GraphNode(
                    referencedObject,
                    resolveObjectType(referencedObject).toString(),
                    referencedObject
            );
            GraphEdge edge = new GraphEdge(fromNode, toNode, EdgeType.REFERENCES.toString());
            edges.add(edge);
        }
        return edges;
    }

    private String extractEntityApiName(Object entityDefinitionValue) {
        if (!(entityDefinitionValue instanceof Map<?, ?> entityDefinition)) {
            return null;
        }
        return stringValue(entityDefinition.get("QualifiedApiName"));
    }

    private String extractFirstReferenceTarget(Map<String, Object> fieldDefinition) {
        Object referenceToValue = fieldDefinition.get("ReferenceTo");
        if (!(referenceToValue instanceof Map<?, ?> referenceToMap)) {
            return null;
        }

        Object targetsValue = referenceToMap.get("referenceTo");
        if (!(targetsValue instanceof List<?> targets)) {
            return null;
        }

        return targets.stream()
                .map(this::stringValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private NodeType resolveObjectType(String objectApiName) {
        return objectApiName != null && objectApiName.endsWith("__c")
                ? NodeType.CUSTOM_OBJECT
                : NodeType.STANDARD_OBJECT;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
