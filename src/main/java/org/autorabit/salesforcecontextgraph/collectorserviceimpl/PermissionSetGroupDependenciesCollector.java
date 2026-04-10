package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.PermissionSetGroup;
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

import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
public class PermissionSetGroupDependenciesCollector implements CollectorService {

    private final MetadataReaderService metadataReaderService;
    private final MetadataDependencyRepository metadataDependencyRepository;
    private final MetadataApiClient metadataApiClient;

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        return buildRelativeGraphEdges(null);
    }

    public List<GraphEdge> buildRelativeGraphEdges(SalesforceSession session) {
        List<GraphEdge> edges = new ArrayList<>();
        List<String> permissionSetGroupApiNames = metadataReaderService.listMetadataObjects("PermissionSetGroup", session);
        List<Metadata> permissionSetGroupMetadata = metadataReaderService.getMetaDataDescribe(
                new MetadataDescribeRequestDto("PermissionSetGroup", permissionSetGroupApiNames),
                session
        );
        for (Metadata metadataRecord : permissionSetGroupMetadata) {
            if (!(metadataRecord instanceof PermissionSetGroup permissionSetGroup)) {
                continue;
            }
            String[] permissionSets = permissionSetGroup.getPermissionSets();
            if (permissionSets == null) {
                continue;
            }

            GraphNode fromNode = GraphNode.buildGraphNode(permissionSetGroup.getFullName(), NodeType.PERMISSION_SET_GROUP.toString());
            for(String permissionSet : permissionSets) {
                GraphNode toNode = GraphNode.buildGraphNode(permissionSet, NodeType.PERMISSION_SET.toString());
                edges.add (
                        new GraphEdge (
                                fromNode,
                                toNode,
                                EdgeResolverService.resolve(NodeType.PERMISSION_SET_GROUP.getMetadatatype(), NodeType.PERMISSION_SET.getMetadatatype()).toString()
                        )
                );
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
                .map(edge -> Helper.buildMetadataDependency(edge, orgId, "PERMISSION_SET_GROUP_DEPENDENCIES_COLLECTOR"))
                .toList();
        metadataDependencyRepository.saveAll(metadataDependencies);
    }
}
