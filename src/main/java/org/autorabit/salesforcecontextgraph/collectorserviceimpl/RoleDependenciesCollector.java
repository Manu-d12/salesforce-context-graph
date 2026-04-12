package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.Role;
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
public class RoleDependenciesCollector implements CollectorService {

    private static final String METADATA_TYPE = "Role";
    private static final String EDGE_SOURCE = "ROLE_DEPENDENCIES_COLLECTOR";

    private final MetadataReaderService metadataReaderService;
    private final MetadataDependencyRepository metadataDependencyRepository;
    private final MetadataApiClient metadataApiClient;

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        return buildRelativeGraphEdges(null);
    }

    public List<GraphEdge> buildRelativeGraphEdges(SalesforceSession session) {
        List<String> roleApiNames = metadataReaderService.listMetadataObjects(METADATA_TYPE, session);
        if (roleApiNames.isEmpty()) {
            return List.of();
        }

        List<Metadata> roleMetadata = metadataReaderService.getMetaDataDescribe(
                new MetadataDescribeRequestDto(METADATA_TYPE, roleApiNames),
                session
        );

        List<GraphEdge> edges = new ArrayList<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        for (Metadata metadataRecord : roleMetadata) {
            if (!(metadataRecord instanceof Role role)) {
                continue;
            }
            addParentRoleEdge(role, edges, edgeKeys);
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

    private void addParentRoleEdge(Role role, List<GraphEdge> edges, Set<String> edgeKeys) {
        if (!hasText(role.getFullName()) || !hasText(role.getParentRole())) {
            return;
        }

        GraphNode childRoleNode = GraphNode.buildGraphNode(role.getFullName(), NodeType.ROLE.toString());
        GraphNode parentRoleNode = GraphNode.buildGraphNode(role.getParentRole(), NodeType.ROLE.toString());
        String edgeType = EdgeResolverService.resolve(
                NodeType.ROLE.getMetadatatype(),
                NodeType.ROLE.getMetadatatype()
        ).toString();
        String edgeKey = childRoleNode.id() + "|" + parentRoleNode.id() + "|" + edgeType;
        if (edgeKeys.add(edgeKey)) {
            edges.add(new GraphEdge(childRoleNode, parentRoleNode, edgeType));
        }
    }

    @PostConstruct
    public void init() {
        this.persistRelativeGraphEdges(null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
