package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import com.sforce.soap.metadata.DescribeMetadataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.autorabit.salesforcecontextgraph.collectorservice.CollectorService;
import org.autorabit.salesforcecontextgraph.domain.enums.EdgeType;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;
import org.autorabit.salesforcecontextgraph.integration.salesforce.ToolingApiClient;
import org.springframework.stereotype.Service;

@Service
public class CustomStandardObjectDependencyCollector implements CollectorService {

    private final MetadataApiClient metadataApiClient;
    private final ToolingApiClient toolingApiClient;

    public CustomStandardObjectDependencyCollector(
            MetadataApiClient metadataApiClient,
            ToolingApiClient toolingApiClient
    ) {
        this.metadataApiClient = metadataApiClient;
        this.toolingApiClient = toolingApiClient;
    }

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        List<String> customFields = listMetadataFullNames("CustomField");
        List<Map<String, Object>> fieldDefinitions = getFieldDefinitions(customFields);

        List<GraphEdge> edges = new ArrayList<>();
        for (Map<String, Object> fieldDefinition : fieldDefinitions) {
            String fieldName = stringValue(fieldDefinition.get("QualifiedApiName"));
            String objectApiName = extractEntityApiName(fieldDefinition.get("EntityDefinition"));
            if (objectApiName == null) {
                continue;
            }

            GraphNode fromNode = new GraphNode(
                    objectApiName + "." + fieldName + " - " + NodeType.CUSTOM_FIELD,
                    NodeType.CUSTOM_FIELD.toString(),
                    fieldName
            );

            GraphNode toParentNode = new GraphNode(
                    objectApiName + " - " + NodeType.STANDARD_OBJECT,
                    resolveObjectType(fieldName).toString(),
                    objectApiName + " - " + NodeType.STANDARD_OBJECT
            );
            edges.add(new GraphEdge(fromNode, toParentNode, EdgeType.REFERENCES.toString()));

            String referencedObject = extractFirstReferenceTarget(fieldDefinition);
            if (referencedObject == null) {
                continue;
            }

            GraphNode toReferencedNode = new GraphNode(
                    referencedObject + " - " + resolveObjectType(referencedObject),
                    resolveObjectType(referencedObject).toString(),
                    referencedObject + " - " + resolveObjectType(referencedObject)
            );
            edges.add(new GraphEdge(fromNode, toReferencedNode, EdgeType.REFERENCES.toString()));
        }
        return edges;
    }

    public List<String> listMetadataFullNames(String metadataType) {
        return metadataApiClient.listMetadataFullNames(metadataType).stream()
                .sorted()
                .toList();
    }

    public DescribeMetadataResult describeMetadata() {
        return metadataApiClient.describeMetadata();
    }

    public List<Map<String, Object>> getFieldDefinitions(List<String> fieldApiNames) {
        if (fieldApiNames == null || fieldApiNames.isEmpty()) {
            throw new IllegalArgumentException("fieldApiNames is required");
        }

        List<String> normalizedFieldApiNames = fieldApiNames.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
        if (normalizedFieldApiNames.isEmpty()) {
            throw new IllegalArgumentException("fieldApiNames is required");
        }

        List<String> fieldNames = normalizedFieldApiNames.stream()
                .map(this::extractFieldName)
                .distinct()
                .toList();
        List<String> objectNames = normalizedFieldApiNames.stream()
                .map(this::extractObjectName)
                .distinct()
                .toList();

        return toolingApiClient.query("""
                SELECT Id, ReferenceTo, QualifiedApiName, DataType, Label, EntityDefinition.QualifiedApiName
                FROM FieldDefinition
                WHERE QualifiedApiName IN (%s)
                AND EntityDefinition.QualifiedApiName IN (%s)
                """.formatted(toQuotedSoqlList(fieldNames), toQuotedSoqlList(objectNames)));
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

    private String extractFieldName(String value) {
        int separatorIndex = value.indexOf('.');
        if (separatorIndex < 0 || separatorIndex == value.length() - 1) {
            throw new IllegalArgumentException("fieldApiNames must be in Object.Field format");
        }
        return value.substring(separatorIndex + 1);
    }

    private String extractObjectName(String value) {
        int separatorIndex = value.indexOf('.');
        if (separatorIndex <= 0) {
            throw new IllegalArgumentException("fieldApiNames must be in Object.Field format");
        }
        return value.substring(0, separatorIndex);
    }

    private String toQuotedSoqlList(List<String> values) {
        return values.stream()
                .map(this::escapeSoql)
                .map(value -> "'" + value + "'")
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow(() -> new IllegalArgumentException("fieldApiNames is required"));
    }

    private String escapeSoql(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
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
