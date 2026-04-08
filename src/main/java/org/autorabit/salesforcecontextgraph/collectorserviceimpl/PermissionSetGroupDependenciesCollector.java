package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.PermissionSetGroup;
import lombok.AllArgsConstructor;
import org.autorabit.salesforcecontextgraph.api.request.MetadataDescribeRequestDto;
import org.autorabit.salesforcecontextgraph.collectorservice.CollectorService;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.service.EdgeResolverService;
import org.autorabit.salesforcecontextgraph.service.MetadataReaderService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
public class PermissionSetGroupDependenciesCollector implements CollectorService {

    private final MetadataReaderService metadataReaderService;

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        List<GraphEdge> edges = new ArrayList<>();
        List<String> permissionSetGroupApiNames = metadataReaderService.listMetadataObjects("PermissionSetGroup");
        List<Metadata> permissionSetGroupMetadata = metadataReaderService.getMetaDataDescribe(
                new MetadataDescribeRequestDto("PermissionSetGroup", permissionSetGroupApiNames)
        );
        for (Metadata metadataRecord : permissionSetGroupMetadata) {
            if (!(metadataRecord instanceof PermissionSetGroup permissionSetGroup)) {
                continue;
            }
            String[] permissionSets = permissionSetGroup.getPermissionSets();
            if (permissionSets == null) {
                continue;
            }

            GraphNode fromNode = GraphNode.buildGraphNode(permissionSetGroup.getFullName(), NodeType.PERMISSION_SET_GROUP.toString(), permissionSetGroup.getFullName());
            for(String permissionSet : permissionSets) {
                GraphNode toNode = GraphNode.buildGraphNode(permissionSet, NodeType.PERMISSION_SET.toString(), permissionSet);
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
    public void persistRelativeGraphEdges() {

    }
}
