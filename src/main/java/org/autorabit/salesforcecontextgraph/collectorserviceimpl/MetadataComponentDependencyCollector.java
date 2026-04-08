package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.config.SalesforceIntegrationProperties;
import org.autorabit.salesforcecontextgraph.collectorservice.CollectorService;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.integration.salesforce.ToolingApiClient;
import org.autorabit.salesforcecontextgraph.service.EdgeResolverService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MetadataComponentDependencyCollector implements CollectorService {

    private final ToolingApiClient toolingApiClient;
    private final SalesforceIntegrationProperties properties;

    public MetadataComponentDependencyCollector(
            ToolingApiClient toolingApiClient,
            SalesforceIntegrationProperties properties
    ) {
        this.toolingApiClient = toolingApiClient;
        this.properties = properties;
    }

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        if ("stub".equalsIgnoreCase(properties.getFetchMode())) {
            return fetchStubMetadata();
        }
        return fetchDependencyMetadata();
    }

    @Async("loadDependenciesExecutor")
    @Override
    public void persistRelativeGraphEdges(SfOrgSyncRequestDto requestDto) {

    }

    private List<GraphEdge> fetchDependencyMetadata() {
        List<Map<String, Object>> dependencyRows = toolingApiClient.query("""
                SELECT MetadataComponentId, MetadataComponentName, MetadataComponentType,
                       RefMetadataComponentId, RefMetadataComponentName, RefMetadataComponentType
                FROM MetadataComponentDependency
               """);

        List<GraphEdge> edges = new ArrayList<>();

        for (Map<String, Object> row : dependencyRows) {
            String metadataId = stringValue(row, "MetadataComponentId");
            String metadataName = stringValue(row, "MetadataComponentName");
            String metadataType = stringValue(row, "MetadataComponentType");
            String refId = stringValue(row, "RefMetadataComponentId");
            String refName = stringValue(row, "RefMetadataComponentName");
            String refType = stringValue(row, "RefMetadataComponentType");

            if (metadataId == null || metadataName == null || metadataType == null
                    || refId == null || refName == null || refType == null) {
                continue;
            }

            String parentMetadataType = NodeType.getNodeType(metadataType) != null
                    ? NodeType.getNodeType(metadataType).toString()
                    : metadataType;
            String childMetadataType = NodeType.getNodeType(refType) != null
                    ? NodeType.getNodeType(refType).toString()
                    : refType;

            edges.add(new GraphEdge(
                    new GraphNode(metadataId, parentMetadataType, metadataName),
                    new GraphNode(refId, childMetadataType, refName),
                    EdgeResolverService.resolve(metadataType, refType).toString()
            ));
        }
        return edges;
    }

    private List<GraphEdge> fetchStubMetadata() {
        String fallbackName = "FULL_GRAPH";
        GraphNode fromNode = new GraphNode(fallbackName, "CUSTOM_OBJECT", fallbackName);
        GraphNode toNode = new GraphNode(fallbackName + ".SyntheticField__c", "FIELD", fallbackName + ".SyntheticField__c");
        return List.of(new GraphEdge(fromNode, toNode, "DEPENDS_ON"));
    }

    private String stringValue(Map<String, Object> record, String key) {
        Object value = record.get(key);
        return value == null ? null : value.toString();
    }
}
