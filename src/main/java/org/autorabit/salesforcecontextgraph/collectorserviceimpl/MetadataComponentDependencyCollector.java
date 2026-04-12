package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.config.SalesforceIntegrationProperties;
import org.autorabit.salesforcecontextgraph.collectorservice.CollectorService;
import org.autorabit.salesforcecontextgraph.db_entities.MetadataDependency;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;
import org.autorabit.salesforcecontextgraph.integration.salesforce.ToolingApiClient;
import org.autorabit.salesforcecontextgraph.repository.MetadataDependencyRepository;
import org.autorabit.salesforcecontextgraph.service.EdgeResolverService;
import org.autorabit.salesforcecontextgraph.service.MetadataReaderService;
import org.autorabit.salesforcecontextgraph.utils.Helper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MetadataComponentDependencyCollector implements CollectorService {

    private final ToolingApiClient toolingApiClient;
    private final MetadataApiClient metadataApiClient;
    private final MetadataDependencyRepository metadataDependencyRepository;
    private final SalesforceIntegrationProperties properties;
    private final MetadataReaderService readerService;

    public MetadataComponentDependencyCollector (
            ToolingApiClient toolingApiClient,
            MetadataApiClient metadataApiClient,
            MetadataDependencyRepository metadataDependencyRepository,
            SalesforceIntegrationProperties properties,
            MetadataReaderService readerService
    ) {
        this.toolingApiClient = toolingApiClient;
        this.metadataApiClient = metadataApiClient;
        this.metadataDependencyRepository = metadataDependencyRepository;
        this.properties = properties;
        this.readerService = readerService;
    }

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        return buildRelativeGraphEdges(null);
    }

    public List<GraphEdge> buildRelativeGraphEdges(SalesforceSession session) {
        if ("stub".equalsIgnoreCase(properties.getFetchMode())) {
            return fetchStubMetadata();
        }
        return fetchDependencyMetadata(session);
    }


    public void persistRelativeGraphEdges(SfOrgSyncRequestDto requestDto) {
        persistRelativeGraphEdges(requestDto, null);
    }

    @Async("loadDependenciesExecutor")
    @Override
    public void persistRelativeGraphEdges(SfOrgSyncRequestDto requestDto, SalesforceSession session) {
        List<GraphEdge> edges = buildRelativeGraphEdges(session);
        if (edges.isEmpty()) {
            return;
        }

        String orgId = Helper.resolveOrgId(metadataApiClient, session);
        List<MetadataDependency> metadataDependencies = edges.stream()
                .map(edge -> Helper.buildMetadataDependency(edge, orgId, "METADATA_COMPONENT_DEPENDENCY_COLLECTOR"))
                .toList();
        metadataDependencyRepository.saveAll(metadataDependencies);
    }



    private List<GraphEdge> fetchDependencyMetadata(SalesforceSession session) {
        List<Map<String, Object>> dependencyRows = toolingApiClient.query("""
                SELECT MetadataComponentId, MetadataComponentName, MetadataComponentType,
                       RefMetadataComponentId, RefMetadataComponentName, RefMetadataComponentType
                FROM MetadataComponentDependency
               """, session);

        List<GraphEdge> edges = new ArrayList<>();

        Map<String, String> componentIdFullNameMap = new HashMap<>();

        for (Map<String, Object> row : dependencyRows) {
            try {
                String metadataId = stringValue(row, "MetadataComponentId");
                String metadataName = stringValue(row, "MetadataComponentName");
                String metadataType = stringValue(row, "MetadataComponentType").equals("StandardEntity") ? "CustomObject" : stringValue(row, "MetadataComponentType");
                String refId = stringValue(row, "RefMetadataComponentId");
                String refName = stringValue(row, "RefMetadataComponentName");
                String refType = stringValue(row, "RefMetadataComponentType").equals("StandardEntity") ? "CustomObject" : stringValue(row, "RefMetadataComponentType");

                if (metadataId == null || metadataName == null || metadataType == null
                        || refId == null || refName == null || refType == null) {
                    continue;
                }

                if(!componentIdFullNameMap.containsKey(metadataId)) {
                    List<MetadataApiClient.MetadataIdentifier> metadataIdentifiers = readerService.listMetadataIdentifiers(metadataType, session);
                    metadataIdentifiers.forEach(metadataIdentifier -> componentIdFullNameMap.put(metadataIdentifier.id(), metadataIdentifier.fullName()));
                }

                if(!componentIdFullNameMap.containsKey(refId)) {
                    List<MetadataApiClient.MetadataIdentifier> metadataIdentifiers = readerService.listMetadataIdentifiers(refType, session);
                    metadataIdentifiers.forEach(metadataIdentifier -> componentIdFullNameMap.put(metadataIdentifier.id(), metadataIdentifier.fullName()));
                }

                String parentMetadataType = NodeType.getNodeType(metadataType) != null
                        ? NodeType.getNodeType(metadataType).toString()
                        : metadataType;
                String childMetadataType = NodeType.getNodeType(refType) != null
                        ? NodeType.getNodeType(refType).toString()
                        : refType;

                String fromNodeFullName = componentIdFullNameMap.get(metadataId) == null ? metadataName : componentIdFullNameMap.get(metadataId);
                String toNodeFullName = componentIdFullNameMap.get(refId) == null ? refName : componentIdFullNameMap.get(refId);

                edges.add(new GraphEdge(
                        GraphNode.buildGraphNode(fromNodeFullName, parentMetadataType),
                        GraphNode.buildGraphNode(toNodeFullName, childMetadataType),
                        EdgeResolverService.resolve(metadataType, refType).toString()
                ));
            } catch (Exception ignored) {}
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
