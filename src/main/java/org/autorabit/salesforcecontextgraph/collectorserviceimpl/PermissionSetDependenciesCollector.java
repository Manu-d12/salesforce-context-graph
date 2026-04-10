package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.PermissionSet;
import com.sforce.soap.metadata.PermissionSetApexClassAccess;
import com.sforce.soap.metadata.PermissionSetApplicationVisibility;
import com.sforce.soap.metadata.PermissionSetCustomMetadataTypeAccess;
import com.sforce.soap.metadata.PermissionSetCustomSettingAccess;
import com.sforce.soap.metadata.PermissionSetFieldPermissions;
import com.sforce.soap.metadata.PermissionSetFlowAccess;
import com.sforce.soap.metadata.PermissionSetObjectPermissions;
import com.sforce.soap.metadata.PermissionSetTabSetting;
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
public class PermissionSetDependenciesCollector implements CollectorService {
    private final MetadataReaderService metadataReaderService;
    private final MetadataDependencyRepository metadataDependencyRepository;
    private final MetadataApiClient metadataApiClient;

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        return buildRelativeGraphEdges(null);
    }

    public List<GraphEdge> buildRelativeGraphEdges(SalesforceSession session) {
        List<String> permissionSetApiNames = metadataReaderService.listMetadataObjects("PermissionSet", session);
        List<Metadata> permissionSetMetadata = metadataReaderService.getMetaDataDescribe(
                new MetadataDescribeRequestDto("PermissionSet", permissionSetApiNames),
                session
        );

        List<GraphEdge> edges = new ArrayList<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        for (Metadata metadataRecord : permissionSetMetadata) {
            if (!(metadataRecord instanceof PermissionSet permissionSet)) {
                continue;
            }

            GraphNode permissionSetNode = GraphNode.buildGraphNode(permissionSet.getFullName(), NodeType.PERMISSION_SET.toString());
            processApplicationVisibilities(permissionSetNode, permissionSet.getApplicationVisibilities(), edges, edgeKeys);
            processClassAccesses(permissionSetNode, permissionSet.getClassAccesses(), edges, edgeKeys);
            processCustomMetadataTypeAccesses(permissionSetNode, permissionSet.getCustomMetadataTypeAccesses(), edges, edgeKeys);
            processCustomSettingAccesses(permissionSetNode, permissionSet.getCustomSettingAccesses(), edges, edgeKeys);
            processFieldPermissions(permissionSetNode, permissionSet.getFieldPermissions(), edges, edgeKeys);
            processObjectPermissions(permissionSetNode, permissionSet.getObjectPermissions(), edges, edgeKeys);
            processFlowAccesses(permissionSetNode, permissionSet.getFlowAccesses(), edges, edgeKeys);
            processTabSettings(permissionSetNode, permissionSet.getTabSettings(), edges, edgeKeys);
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
        String orgId = Helper.resolveOrgId(metadataApiClient, session);
        List<MetadataDependency> metadataDependencies = edges.stream()
                .map(edge -> Helper.buildMetadataDependency(edge, orgId, "PERMISSION_SET_DEPENDENCIES_COLLECTOR"))
                .toList();

        metadataDependencyRepository.saveAll(metadataDependencies);
    }


    private void processApplicationVisibilities(
            GraphNode permissionSetNode,
            PermissionSetApplicationVisibility[] visibilities,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (visibilities == null) {
            return;
        }
        for (PermissionSetApplicationVisibility visibility : visibilities) {
            if (visibility == null) {
                continue;
            }
            addEdge(permissionSetNode, visibility.getApplication(), NodeType.CUSTOM_APPLICATION, edges, edgeKeys);
        }
    }

    private void processClassAccesses(
            GraphNode permissionSetNode,
            PermissionSetApexClassAccess[] classAccesses,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (classAccesses == null) {
            return;
        }
        for (PermissionSetApexClassAccess classAccess : classAccesses) {
            if (classAccess == null) {
                continue;
            }
            addEdge(permissionSetNode, classAccess.getApexClass(), NodeType.APEX_CLASS, edges, edgeKeys);
        }
    }

    private void processCustomMetadataTypeAccesses(
            GraphNode permissionSetNode,
            PermissionSetCustomMetadataTypeAccess[] customMetadataTypeAccesses,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (customMetadataTypeAccesses == null) {
            return;
        }
        for (PermissionSetCustomMetadataTypeAccess access : customMetadataTypeAccesses) {
            if (access == null) {
                continue;
            }
            addEdge(permissionSetNode, access.getName(), NodeType.CUSTOM_METADATA_TYPE, edges, edgeKeys);
        }
    }

    private void processCustomSettingAccesses(
            GraphNode permissionSetNode,
            PermissionSetCustomSettingAccess[] customSettingAccesses,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (customSettingAccesses == null) {
            return;
        }
        for (PermissionSetCustomSettingAccess access : customSettingAccesses) {
            if (access == null) {
                continue;
            }
            addEdge(permissionSetNode, access.getName(), NodeType.CUSTOM_SETTINGS, edges, edgeKeys);
        }
    }

    private void processFieldPermissions(
            GraphNode permissionSetNode,
            PermissionSetFieldPermissions[] fieldPermissions,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (fieldPermissions == null) {
            return;
        }
        for (PermissionSetFieldPermissions fieldPermission : fieldPermissions) {
            if (fieldPermission == null) {
                continue;
            }
            addEdge(permissionSetNode, fieldPermission.getField(), NodeType.CUSTOM_FIELD, edges, edgeKeys);
        }
    }

    private void processObjectPermissions(
            GraphNode permissionSetNode,
            PermissionSetObjectPermissions[] objectPermissions,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (objectPermissions == null) {
            return;
        }
        for (PermissionSetObjectPermissions objectPermission : objectPermissions) {
            if (objectPermission == null) {
                continue;
            }
            addEdge(
                    permissionSetNode,
                    objectPermission.getObject(),
                    resolveObjectType(objectPermission.getObject()),
                    edges,
                    edgeKeys
            );
        }
    }

    private void processFlowAccesses(
            GraphNode permissionSetNode,
            PermissionSetFlowAccess[] flowAccesses,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (flowAccesses == null) {
            return;
        }
        for (PermissionSetFlowAccess flowAccess : flowAccesses) {
            if (flowAccess == null) {
                continue;
            }
            addEdge(permissionSetNode, flowAccess.getFlow(), NodeType.FLOW, edges, edgeKeys);
        }
    }

    private void processTabSettings(
            GraphNode permissionSetNode,
            PermissionSetTabSetting[] tabSettings,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (tabSettings == null) {
            return;
        }
        for (PermissionSetTabSetting tabSetting : tabSettings) {
            if (tabSetting == null) {
                continue;
            }
            addEdge(permissionSetNode, tabSetting.getTab(), NodeType.CUSTOM_TAB, edges, edgeKeys);
        }
    }

    private void addEdge(
            GraphNode permissionSetNode,
            String targetName,
            NodeType targetType,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        GraphNode targetNode = new GraphNode(targetName, targetType.toString(), targetName);
        String edgeType = EdgeResolverService.resolve(
                NodeType.PERMISSION_SET.getMetadatatype(),
                targetType.getMetadatatype()
        ).toString();
        String edgeKey = permissionSetNode.id() + "|" + targetNode.id() + "|" + edgeType;
        if (edgeKeys.add(edgeKey)) {
            edges.add(new GraphEdge(permissionSetNode, targetNode, edgeType));
        }
    }

    private NodeType resolveObjectType(String objectName) {
        return objectName.endsWith("__c") ? NodeType.CUSTOM_OBJECT : NodeType.STANDARD_OBJECT;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
