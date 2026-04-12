package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import com.sforce.soap.metadata.CustomPermission;
import com.sforce.soap.metadata.CustomPermissionDependencyRequired;
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
public class CustomPermissionDependenciesCollector implements CollectorService {

    private static final String METADATA_TYPE = "CustomPermission";
    private static final String EDGE_SOURCE = "CUSTOM_PERMISSION_DEPENDENCIES_COLLECTOR";

    private final MetadataReaderService metadataReaderService;
    private final MetadataDependencyRepository metadataDependencyRepository;
    private final MetadataApiClient metadataApiClient;

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        return buildRelativeGraphEdges(null);
    }

    public List<GraphEdge> buildRelativeGraphEdges(SalesforceSession session) {
        List<String> customPermissionApiNames = metadataReaderService.listMetadataObjects(METADATA_TYPE, session);
        if (customPermissionApiNames.isEmpty()) {
            return List.of();
        }

        List<Metadata> customPermissionMetadata = metadataReaderService.getMetaDataDescribe(
                new MetadataDescribeRequestDto(METADATA_TYPE, customPermissionApiNames),
                session
        );

        List<GraphEdge> edges = new ArrayList<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        for (Metadata metadataRecord : customPermissionMetadata) {
            try {
                if (!(metadataRecord instanceof CustomPermission customPermission)) {
                    continue;
                }
                addRequiredPermissionEdges(customPermission, edges, edgeKeys);
            } catch (Exception ignored) {}
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
                .map(edge -> Helper.buildMetadataDependency(edge, orgId, EDGE_SOURCE))
                .toList();
        metadataDependencyRepository.saveAll(metadataDependencies);
    }

    private void addRequiredPermissionEdges(
            CustomPermission customPermission,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (!hasText(customPermission.getFullName())) {
            return;
        }

        CustomPermissionDependencyRequired[] requiredPermissions = customPermission.getRequiredPermission();
        if (requiredPermissions == null) {
            return;
        }

        GraphNode customPermissionNode = GraphNode.buildGraphNode(
                customPermission.getFullName(),
                NodeType.CUSTOM_PERMISSION.toString()
        );
        String edgeType = EdgeResolverService.resolve(
                NodeType.CUSTOM_PERMISSION.getMetadatatype(),
                NodeType.CUSTOM_PERMISSION.getMetadatatype()
        ).toString();

        for (CustomPermissionDependencyRequired requiredPermission : requiredPermissions) {
            if (requiredPermission == null
                    || !requiredPermission.isDependency()
                    || !hasText(requiredPermission.getCustomPermission())) {
                continue;
            }

            GraphNode parentPermissionNode = GraphNode.buildGraphNode(
                    requiredPermission.getCustomPermission(),
                    NodeType.CUSTOM_PERMISSION.toString()
            );
            String edgeKey = customPermissionNode.id() + "|" + parentPermissionNode.id() + "|" + edgeType;
            if (edgeKeys.add(edgeKey)) {
                edges.add(new GraphEdge(customPermissionNode, parentPermissionNode, edgeType));
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
