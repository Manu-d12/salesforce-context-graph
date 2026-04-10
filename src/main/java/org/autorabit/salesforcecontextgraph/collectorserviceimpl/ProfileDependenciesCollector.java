package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.Profile;
import com.sforce.soap.metadata.ProfileApexClassAccess;
import com.sforce.soap.metadata.ProfileApexPageAccess;
import com.sforce.soap.metadata.ProfileApplicationVisibility;
import com.sforce.soap.metadata.ProfileCustomMetadataTypeAccess;
import com.sforce.soap.metadata.ProfileCustomPermissions;
import com.sforce.soap.metadata.ProfileCustomSettingAccess;
import com.sforce.soap.metadata.ProfileFieldLevelSecurity;
import com.sforce.soap.metadata.ProfileFlowAccess;
import com.sforce.soap.metadata.ProfileLayoutAssignment;
import com.sforce.soap.metadata.ProfileObjectPermissions;
import com.sforce.soap.metadata.ProfileTabVisibility;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProfileDependenciesCollector implements CollectorService {

    private final MetadataReaderService metadataReaderService;
    private final MetadataDependencyRepository metadataDependencyRepository;
    private final MetadataApiClient metadataApiClient;
    @Qualifier("loadDependenciesExecutor")
    private final ThreadPoolTaskExecutor loadDependenciesExecutor;

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        return buildRelativeGraphEdges(null);
    }

    public List<GraphEdge> buildRelativeGraphEdges(SalesforceSession session) {
        List<String> profileApiNames = metadataReaderService.listMetadataObjects("Profile", session);
        List<CompletableFuture<List<GraphEdge>>> profileTasks = new ArrayList<>();
        for (String profileApiName : profileApiNames) {
            profileTasks.add(CompletableFuture.supplyAsync(
                    () -> fetchAndBuildProfileEdges(profileApiName, session),
                    loadDependenciesExecutor
            ));
        }

        List<GraphEdge> edges = new ArrayList<>();
        for (CompletableFuture<List<GraphEdge>> profileTask : profileTasks) {
            edges.addAll(profileTask.join());
        }
        return edges;
    }

    @Async("loadDependenciesExecutor")
    @Override
    public void persistRelativeGraphEdges(SfOrgSyncRequestDto requestDto) {
        persistRelativeGraphEdges(requestDto, null);
    }

    public void persistRelativeGraphEdges(SfOrgSyncRequestDto requestDto, SalesforceSession session) {
        String orgId = Helper.resolveOrgId(metadataApiClient, session);
        List<String> profileApiNames = metadataReaderService.listMetadataObjects("Profile", session);
        List<CompletableFuture<Void>> profileTasks = new ArrayList<>();
        for (String profileApiName : profileApiNames) {
            profileTasks.add(CompletableFuture.runAsync(
                    () -> fetchProcessAndPersistProfile(profileApiName, orgId, session),
                    loadDependenciesExecutor
            ));
        }

        for (CompletableFuture<Void> profileTask : profileTasks) {
            profileTask.join();
        }
    }

    private List<GraphEdge> fetchAndBuildProfileEdges(String profileApiName, SalesforceSession session) {
        Profile profile = fetchProfile(profileApiName, session);
        if (profile == null || !profile.isCustom()) {
            return List.of();
        }
        return buildProfileEdges(profile);
    }

    private void fetchProcessAndPersistProfile(String profileApiName, String orgId, SalesforceSession session) {
        List<GraphEdge> edges = fetchAndBuildProfileEdges(profileApiName, session);
        if (edges.isEmpty()) {
            return;
        }

        List<MetadataDependency> metadataDependencies = edges.stream()
                .map(edge -> Helper.buildMetadataDependency(edge, orgId, "PROFILE_DEPENDENCIES_COLLECTOR"))
                .toList();
        metadataDependencyRepository.saveAll(metadataDependencies);
    }

    private Profile fetchProfile(String profileApiName, SalesforceSession session) {
        List<Metadata> profileMetadata = metadataReaderService.getMetaDataDescribe(
                new MetadataDescribeRequestDto("Profile", List.of(profileApiName)),
                session
        );

        for (Metadata metadataRecord : profileMetadata) {
            if (metadataRecord instanceof Profile profile) {
                return profile;
            }
        }
        return null;
    }

    private List<GraphEdge> buildProfileEdges(Profile profile) {
        List<GraphEdge> edges = new ArrayList<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        GraphNode profileNode = GraphNode.buildGraphNode(profile.getFullName(), NodeType.PROFILE.toString());

        processApplicationVisibilities(profileNode, profile.getApplicationVisibilities(), edges, edgeKeys);
        processClassAccesses(profileNode, profile.getClassAccesses(), edges, edgeKeys);
        processCustomMetadataTypeAccesses(profileNode, profile.getCustomMetadataTypeAccesses(), edges, edgeKeys);
        processCustomPermissions(profileNode, profile.getCustomPermissions(), edges, edgeKeys);
        processCustomSettingAccesses(profileNode, profile.getCustomSettingAccesses(), edges, edgeKeys);
        processFieldLevelSecurities(profileNode, profile.getFieldPermissions(), edges, edgeKeys);
        processFieldPermissions(profileNode, profile.getFieldPermissions(), edges, edgeKeys);
        processFlowAccesses(profileNode, profile.getFlowAccesses(), edges, edgeKeys);
        processLayoutAssignments(profileNode, profile.getLayoutAssignments(), edges, edgeKeys);
        processObjectPermissions(profileNode, profile.getObjectPermissions(), edges, edgeKeys);
        processPageAccesses(profileNode, profile.getPageAccesses(), edges, edgeKeys);
        processTabVisibilities(profileNode, profile.getTabVisibilities(), edges, edgeKeys);

        return edges;
    }

    private void processApplicationVisibilities(
            GraphNode profileNode,
            ProfileApplicationVisibility[] visibilities,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (visibilities == null) {
            return;
        }
        for (ProfileApplicationVisibility visibility : visibilities) {
            if (visibility == null) {
                continue;
            }
            addEdge(profileNode, visibility.getApplication(), NodeType.CUSTOM_APPLICATION, edges, edgeKeys);
        }
    }

    private void processClassAccesses(
            GraphNode profileNode,
            ProfileApexClassAccess[] classAccesses,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (classAccesses == null) {
            return;
        }
        for (ProfileApexClassAccess classAccess : classAccesses) {
            if (classAccess == null) {
                continue;
            }
            addEdge(profileNode, classAccess.getApexClass(), NodeType.APEX_CLASS, edges, edgeKeys);
        }
    }

    private void processCustomMetadataTypeAccesses(
            GraphNode profileNode,
            ProfileCustomMetadataTypeAccess[] customMetadataTypeAccesses,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (customMetadataTypeAccesses == null) {
            return;
        }
        for (ProfileCustomMetadataTypeAccess access : customMetadataTypeAccesses) {
            if (access == null) {
                continue;
            }
            addEdge(profileNode, access.getName(), NodeType.CUSTOM_METADATA_TYPE, edges, edgeKeys);
        }
    }

    private void processCustomPermissions(
            GraphNode profileNode,
            ProfileCustomPermissions[] customPermissions,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (customPermissions == null) {
            return;
        }
        for (ProfileCustomPermissions customPermission : customPermissions) {
            if (customPermission == null) {
                continue;
            }
            addEdge(profileNode, customPermission.getName(), NodeType.CUSTOM_PERMISSION, edges, edgeKeys);
        }
    }

    private void processCustomSettingAccesses(
            GraphNode profileNode,
            ProfileCustomSettingAccess[] customSettingAccesses,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (customSettingAccesses == null) {
            return;
        }
        for (ProfileCustomSettingAccess access : customSettingAccesses) {
            if (access == null) {
                continue;
            }
            addEdge(profileNode, access.getName(), NodeType.CUSTOM_SETTINGS, edges, edgeKeys);
        }
    }

    private void processFieldLevelSecurities(
            GraphNode profileNode,
            ProfileFieldLevelSecurity[] fieldLevelSecurities,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        processFieldPermissions(profileNode, fieldLevelSecurities, edges, edgeKeys);
    }

    private void processFieldPermissions(
            GraphNode profileNode,
            ProfileFieldLevelSecurity[] fieldPermissions,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (fieldPermissions == null) {
            return;
        }
        for (ProfileFieldLevelSecurity fieldPermission : fieldPermissions) {
            if (fieldPermission == null) {
                continue;
            }
            addEdge(profileNode, fieldPermission.getField(), NodeType.CUSTOM_FIELD, edges, edgeKeys);
        }
    }

    private void processFlowAccesses(
            GraphNode profileNode,
            ProfileFlowAccess[] flowAccesses,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (flowAccesses == null) {
            return;
        }
        for (ProfileFlowAccess flowAccess : flowAccesses) {
            if (flowAccess == null) {
                continue;
            }
            addEdge(profileNode, flowAccess.getFlow(), NodeType.FLOW, edges, edgeKeys);
        }
    }

    private void processLayoutAssignments(
            GraphNode profileNode,
            ProfileLayoutAssignment[] layoutAssignments,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (layoutAssignments == null) {
            return;
        }
        for (ProfileLayoutAssignment layoutAssignment : layoutAssignments) {
            if (layoutAssignment == null) {
                continue;
            }
            addEdge(profileNode, layoutAssignment.getLayout(), NodeType.LAYOUT, edges, edgeKeys);
            addEdge(profileNode, layoutAssignment.getRecordType(), NodeType.RECORD_TYPE, edges, edgeKeys);
        }
    }

    private void processObjectPermissions(
            GraphNode profileNode,
            ProfileObjectPermissions[] objectPermissions,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (objectPermissions == null) {
            return;
        }
        for (ProfileObjectPermissions objectPermission : objectPermissions) {
            if (objectPermission == null) {
                continue;
            }
            addEdge(
                    profileNode,
                    objectPermission.getObject(),
                    resolveObjectType(objectPermission.getObject()),
                    edges,
                    edgeKeys
            );
        }
    }

    private void processPageAccesses(
            GraphNode profileNode,
            ProfileApexPageAccess[] pageAccesses,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (pageAccesses == null) {
            return;
        }
        for (ProfileApexPageAccess pageAccess : pageAccesses) {
            if (pageAccess == null) {
                continue;
            }
            addEdge(profileNode, pageAccess.getApexPage(), NodeType.APEX_PAGE, edges, edgeKeys);
        }
    }

    private void processTabVisibilities(
            GraphNode profileNode,
            ProfileTabVisibility[] tabVisibilities,
            List<GraphEdge> edges,
            Set<String> edgeKeys
    ) {
        if (tabVisibilities == null) {
            return;
        }
        for (ProfileTabVisibility tabVisibility : tabVisibilities) {
            if (tabVisibility == null) {
                continue;
            }
            addEdge(profileNode, tabVisibility.getTab(), NodeType.CUSTOM_TAB, edges, edgeKeys);
        }
    }

    private void addEdge(
            GraphNode profileNode,
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
                NodeType.PROFILE.getMetadatatype(),
                targetType.getMetadatatype()
        ).toString();
        String edgeKey = profileNode.id() + "|" + targetNode.id() + "|" + edgeType;
        if (edgeKeys.add(edgeKey)) {
            edges.add(new GraphEdge(profileNode, targetNode, edgeType));
        }
    }

    private NodeType resolveObjectType(String objectName) {
        if (!hasText(objectName)) {
            return NodeType.STANDARD_OBJECT;
        }
        return objectName.endsWith("__c") ? NodeType.CUSTOM_OBJECT : NodeType.STANDARD_OBJECT;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
