package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import com.sforce.soap.metadata.CustomTab;
import com.sforce.soap.metadata.Metadata;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.autorabit.salesforcecontextgraph.api.request.MetadataDescribeRequestDto;
import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.collectorservice.CollectorService;
import org.autorabit.salesforcecontextgraph.db_entities.MetadataDependency;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;
import org.autorabit.salesforcecontextgraph.repository.MetadataDependencyRepository;
import org.autorabit.salesforcecontextgraph.service.EdgeResolverService;
import org.autorabit.salesforcecontextgraph.service.MetadataReaderService;
import org.autorabit.salesforcecontextgraph.utils.Helper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomTabDependenciesCollector implements CollectorService {

    private final MetadataReaderService metadataReaderService;
    private final MetadataDependencyRepository metadataDependencyRepository;
    private final MetadataApiClient metadataApiClient;

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        return buildRelativeGraphEdges(null);
    }

    public List<GraphEdge> buildRelativeGraphEdges(SalesforceSession session) {
        List<String> tabApiNames = metadataReaderService.listMetadataObjects("CustomTab", session);
        if (tabApiNames.isEmpty()) {
            return List.of();
        }

        List<Metadata> tabMetadata = metadataReaderService.getMetaDataDescribe(
                new MetadataDescribeRequestDto("CustomTab", tabApiNames),
                session
        );

        List<GraphEdge> edges = new ArrayList<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        for (Metadata metadataRecord : tabMetadata) {
            if (!(metadataRecord instanceof CustomTab customTab)) {
                continue;
            }

            GraphNode tabNode = GraphNode.buildGraphNode(customTab.getFullName(), NodeType.CUSTOM_TAB.toString());
            addEdge(tabNode, customTab.getAuraComponent(), NodeType.AURA_COMPONENT, edges, edgeKeys);
            addEdge(tabNode, customTab.getLwcComponent(), NodeType.LWC, edges, edgeKeys);
            addEdge(tabNode, customTab.getFlexiPage(), NodeType.FLEXI_PAGE, edges, edgeKeys);
            addEdge(tabNode, customTab.getPage(), NodeType.APEX_PAGE, edges, edgeKeys);

            if (customTab.isCustomObject() && hasText(customTab.getFullName()) && customTab.getFullName().endsWith("__c")) {
                addEdge(tabNode, customTab.getFullName(), NodeType.CUSTOM_OBJECT, edges, edgeKeys);
            }
        }
        return edges;
    }

    @Async("loadDependenciesExecutor")
    @Override
    public void persistRelativeGraphEdges(SfOrgSyncRequestDto requestDto) {
        persistRelativeGraphEdges(requestDto, null);
    }

    public void persistRelativeGraphEdges(SfOrgSyncRequestDto requestDto, SalesforceSession session) {
        List<GraphEdge> edges = buildRelativeGraphEdges(session);
        if (edges.isEmpty()) {
            return;
        }

        String orgId = Helper.resolveOrgId(metadataApiClient, session);
        List<MetadataDependency> metadataDependencies = edges.stream()
                .map(edge -> Helper.buildMetadataDependency(edge, orgId, "CUSTOM_TAB_DEPENDENCIES_COLLECTOR"))
                .toList();
        metadataDependencyRepository.saveAll(metadataDependencies);
    }

    private void addEdge(
            GraphNode tabNode,
            String targetName,
            NodeType targetType,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (!hasText(targetName)) {
            return;
        }

        GraphNode targetNode = GraphNode.buildGraphNode(targetName, targetType.toString());
        String edgeType = EdgeResolverService.resolve(
                NodeType.CUSTOM_TAB.getMetadatatype(),
                targetType.getMetadatatype()
        ).toString();
        String edgeKey = tabNode.id() + "|" + targetNode.id() + "|" + edgeType;
        if (edgeKeys.add(edgeKey)) {
            edges.add(new GraphEdge(tabNode, targetNode, edgeType));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @PostConstruct
    public void init() {
        this.persistRelativeGraphEdges(null);
    }
}
