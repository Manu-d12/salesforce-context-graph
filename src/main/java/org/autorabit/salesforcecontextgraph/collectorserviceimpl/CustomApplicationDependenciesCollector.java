package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import com.sforce.soap.metadata.CustomApplication;
import com.sforce.soap.metadata.Metadata;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
public class CustomApplicationDependenciesCollector implements CollectorService {

    private final MetadataReaderService metadataReaderService;
    private final MetadataDependencyRepository metadataDependencyRepository;
    private final MetadataApiClient metadataApiClient;

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        return buildRelativeGraphEdges(null);
    }

    public List<GraphEdge> buildRelativeGraphEdges(SalesforceSession session) {
        List<String> applicationApiNames = metadataReaderService.listMetadataObjects("CustomApplication", session);
        if (applicationApiNames.isEmpty()) {
            return List.of();
        }

        MetadataLookups metadataLookups = buildMetadataLookups(session);
        List<Metadata> applicationMetadata = metadataReaderService.getMetaDataDescribe(
                new MetadataDescribeRequestDto("CustomApplication", applicationApiNames),
                session
        );

        List<GraphEdge> edges = new ArrayList<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        for (Metadata metadataRecord : applicationMetadata) {
            try {
                if (!(metadataRecord instanceof CustomApplication customApplication)) {
                    continue;
                }

                GraphNode applicationNode = GraphNode.buildGraphNode(
                        customApplication.getFullName(),
                        NodeType.CUSTOM_APPLICATION.toString()
                );

                processTabReferences(applicationNode, customApplication.getTabs(), metadataLookups, edges, edgeKeys);
                processTabReference(applicationNode, customApplication.getDefaultLandingTab(), metadataLookups, edges, edgeKeys);
                processUtilityBar(applicationNode, customApplication.getUtilityBar(), metadataLookups, edges, edgeKeys);
                processLogo(applicationNode, customApplication.getLogo(), edges, edgeKeys);
            } catch (Exception ignored) {}
        }
        return edges;
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
                .map(edge -> Helper.buildMetadataDependency(edge, orgId, "CUSTOM_APPLICATION_DEPENDENCIES_COLLECTOR"))
                .toList();
        metadataDependencyRepository.saveAll(metadataDependencies);
    }

    private MetadataLookups buildMetadataLookups(SalesforceSession session) {
        return new MetadataLookups(
                new LinkedHashSet<>(metadataReaderService.listMetadataObjects("CustomTab", session)),
                new LinkedHashSet<>(metadataReaderService.listMetadataObjects("FlexiPage", session)),
                new LinkedHashSet<>(metadataReaderService.listMetadataObjects("WebLink", session))
        );
    }

    private void processTabReferences(
            GraphNode applicationNode,
            String[] references,
            MetadataLookups metadataLookups,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (references == null) {
            return;
        }
        for (String reference : references) {
            processTabReference(applicationNode, reference, metadataLookups, edges, edgeKeys);
        }
    }

    private void processTabReference(
            GraphNode applicationNode,
            String reference,
            MetadataLookups metadataLookups,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        ResolvedReference resolvedReference = resolveTabReference(reference, metadataLookups);
        if (resolvedReference == null) {
            return;
        }
        addEdge(applicationNode, resolvedReference.name(), resolvedReference.nodeType(), edges, edgeKeys);
    }

    private void processUtilityBar(
            GraphNode applicationNode,
            String utilityBar,
            MetadataLookups metadataLookups,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (hasText(utilityBar) && metadataLookups.flexiPages().contains(utilityBar)) {
            addEdge(applicationNode, utilityBar, NodeType.FLEXI_PAGE, edges, edgeKeys);
        }
    }

    private void processLogo(
            GraphNode applicationNode,
            String logo,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (hasText(logo)) {
            addEdge(applicationNode, logo, NodeType.CONTENT_ASSET, edges, edgeKeys);
        }
    }

    private ResolvedReference resolveTabReference(String reference, MetadataLookups metadataLookups) {
        if (!hasText(reference)) {
            return null;
        }
        if (metadataLookups.customTabs().contains(reference)) {
            return new ResolvedReference(reference, NodeType.CUSTOM_TAB);
        }
        if (metadataLookups.webLinks().contains(reference)) {
            return new ResolvedReference(reference, NodeType.WEB_LINK);
        }
        if (metadataLookups.flexiPages().contains(reference)) {
            return new ResolvedReference(reference, NodeType.FLEXI_PAGE);
        }
        return null;
    }

    private void addEdge(
            GraphNode applicationNode,
            String targetName,
            NodeType targetType,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        GraphNode targetNode = GraphNode.buildGraphNode(targetName, targetType.toString());
        String edgeType = EdgeResolverService.resolve(
                NodeType.CUSTOM_APPLICATION.getMetadatatype(),
                targetType.getMetadatatype()
        ).toString();
        String edgeKey = applicationNode.id() + "|" + targetNode.id() + "|" + edgeType;
        if (edgeKeys.add(edgeKey)) {
            edges.add(new GraphEdge(applicationNode, targetNode, edgeType));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record MetadataLookups(
            Set<String> customTabs,
            Set<String> flexiPages,
            Set<String> webLinks
    ) {
    }

    private record ResolvedReference(
            String name,
            NodeType nodeType
    ) {
    }
}
