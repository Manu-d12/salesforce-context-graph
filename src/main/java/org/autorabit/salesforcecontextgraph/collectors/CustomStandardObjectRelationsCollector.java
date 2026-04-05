package org.autorabit.salesforcecontextgraph.collectors;

import com.sforce.soap.metadata.DescribeMetadataResult;
import java.util.List;
import java.util.Map;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;
import org.autorabit.salesforcecontextgraph.integration.salesforce.ToolingApiClient;
import org.springframework.stereotype.Component;

@Component
public class CustomStandardObjectRelationsCollector {

    private final MetadataApiClient metadataApiClient;
    private final ToolingApiClient toolingApiClient;

    public CustomStandardObjectRelationsCollector(
            MetadataApiClient metadataApiClient,
            ToolingApiClient toolingApiClient
    ) {
        this.metadataApiClient = metadataApiClient;
        this.toolingApiClient = toolingApiClient;
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
                SELECT Id, QualifiedApiName, DataType, Label, EntityDefinition.QualifiedApiName
                FROM FieldDefinition
                WHERE QualifiedApiName IN (%s)
                AND EntityDefinition.QualifiedApiName IN (%s)
                """.formatted(toQuotedSoqlList(fieldNames), toQuotedSoqlList(objectNames)));
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
}
