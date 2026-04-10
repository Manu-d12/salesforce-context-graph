package org.autorabit.salesforcecontextgraph.service;

import org.autorabit.salesforcecontextgraph.domain.enums.DependencyStrength;
import org.autorabit.salesforcecontextgraph.domain.enums.EdgeType;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;
import org.autorabit.salesforcecontextgraph.domain.model.EdgeResolution;

public final class EdgeResolverService {

    private EdgeResolverService() {

    }

    public static EdgeResolution resolve(String metadataType, String refMetadataType) {
        NodeType from = NodeType.getNodeType(metadataType);
        NodeType to = NodeType.getNodeType(refMetadataType);

        if (from == null || to == null) {
            return new EdgeResolution(EdgeType.UNKNOWN, DependencyStrength.UNKNOWN);
        }

        if (from == NodeType.LWC && to == NodeType.APEX_CLASS) {
            return new EdgeResolution(EdgeType.CALLS, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.LWC && isObjectOrField(to)) {
            return new EdgeResolution(EdgeType.USES, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.LWC && to == NodeType.LWC) {
            return new EdgeResolution(EdgeType.COMPOSES, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.LWC && isUiResource(to)) {
            return new EdgeResolution(EdgeType.REFERENCES, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.LWC && isConfigNode(to)) {
            return new EdgeResolution(EdgeType.CONFIGURES, DependencyStrength.RUNTIME_DEPENDENCY);
        }

        if (from == NodeType.AURA_COMPONENT && to == NodeType.APEX_CLASS) {
            return new EdgeResolution(EdgeType.CALLS, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.AURA_COMPONENT && to == NodeType.LWC) {
            return new EdgeResolution(EdgeType.COMPOSES, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.AURA_COMPONENT && isObjectOrField(to)) {
            return new EdgeResolution(EdgeType.USES, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.AURA_COMPONENT && isUiResource(to)) {
            return new EdgeResolution(EdgeType.REFERENCES, DependencyStrength.SOFT_DEPENDENCY);
        }

        if (from == NodeType.APEX_CLASS && to == NodeType.APEX_CLASS) {
            return new EdgeResolution(EdgeType.CALLS, DependencyStrength.RUNTIME_DEPENDENCY);
        }
        if (from == NodeType.APEX_CLASS && to == NodeType.APEX_TRIGGER) {
            return new EdgeResolution(EdgeType.INVOKES, DependencyStrength.CONDITIONAL_DEPENDENCY);
        }
        if (from == NodeType.APEX_CLASS && isObjectOrField(to)) {
            return new EdgeResolution(EdgeType.ACCESSES, DependencyStrength.RUNTIME_DEPENDENCY);
        }
        if (from == NodeType.APEX_CLASS && isConfigNode(to)) {
            return new EdgeResolution(EdgeType.CONFIGURES, DependencyStrength.RUNTIME_DEPENDENCY);
        }
        if (from == NodeType.APEX_CLASS && isIntegrationNode(to)) {
            return new EdgeResolution(EdgeType.USES, DependencyStrength.RUNTIME_DEPENDENCY);
        }
        if (from == NodeType.APEX_CLASS && to == NodeType.PERMISSION_SET) {
            return new EdgeResolution(EdgeType.REQUIRES_SETUP_ACCESS, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.APEX_CLASS && to == NodeType.PROFILE) {
            return new EdgeResolution(EdgeType.REQUIRES_SETUP_ACCESS, DependencyStrength.SOFT_DEPENDENCY);
        }

        if (from == NodeType.APEX_TRIGGER && isObjectNode(to)) {
            return new EdgeResolution(EdgeType.BELONGS_TO, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.APEX_TRIGGER && to == NodeType.APEX_CLASS) {
            return new EdgeResolution(EdgeType.CALLS, DependencyStrength.RUNTIME_DEPENDENCY);
        }
        if (from == NodeType.APEX_TRIGGER && isConfigNode(to)) {
            return new EdgeResolution(EdgeType.CONFIGURES, DependencyStrength.RUNTIME_DEPENDENCY);
        }

        if (from == NodeType.FLOW && to == NodeType.APEX_CLASS) {
            return new EdgeResolution(EdgeType.INVOKES, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.FLOW && isObjectOrField(to)) {
            return new EdgeResolution(EdgeType.USES, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.FLOW && isConfigNode(to)) {
            return new EdgeResolution(EdgeType.CONFIGURES, DependencyStrength.RUNTIME_DEPENDENCY);
        }
        if (from == NodeType.FLOW && to == NodeType.FLOW) {
            return new EdgeResolution(EdgeType.INVOKES, DependencyStrength.CONDITIONAL_DEPENDENCY);
        }
        if (from == NodeType.FLOW && to == NodeType.RECORD_TYPE) {
            return new EdgeResolution(EdgeType.USES, DependencyStrength.SOFT_DEPENDENCY);
        }

        if (from == NodeType.CUSTOM_FIELD && isObjectNode(to)) {
            return new EdgeResolution(EdgeType.BELONGS_TO, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.CUSTOM_OBJECT && to == NodeType.CUSTOM_FIELD) {
            return new EdgeResolution(EdgeType.CONTAINS, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.STANDARD_OBJECT && to == NodeType.CUSTOM_FIELD) {
            return new EdgeResolution(EdgeType.CONTAINS, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.CUSTOM_OBJECT && to == NodeType.RECORD_TYPE) {
            return new EdgeResolution(EdgeType.CONTAINS, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.STANDARD_OBJECT && to == NodeType.RECORD_TYPE) {
            return new EdgeResolution(EdgeType.CONTAINS, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.CUSTOM_OBJECT && to == NodeType.VALIDATION_RULE) {
            return new EdgeResolution(EdgeType.CONTAINS, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.STANDARD_OBJECT && to == NodeType.VALIDATION_RULE) {
            return new EdgeResolution(EdgeType.CONTAINS, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.CUSTOM_OBJECT && to == NodeType.LAYOUT) {
            return new EdgeResolution(EdgeType.ASSOCIATED_WITH, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.STANDARD_OBJECT && to == NodeType.LAYOUT) {
            return new EdgeResolution(EdgeType.ASSOCIATED_WITH, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.VALIDATION_RULE && isObjectOrField(to)) {
            return new EdgeResolution(EdgeType.USES, DependencyStrength.HARD_DEPENDENCY);
        }

        if (from == NodeType.LAYOUT && to == NodeType.CUSTOM_FIELD) {
            return new EdgeResolution(EdgeType.CONTAINS, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.LAYOUT && isObjectNode(to)) {
            return new EdgeResolution(EdgeType.BELONGS_TO, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.RECORD_TYPE && isObjectNode(to)) {
            return new EdgeResolution(EdgeType.BELONGS_TO, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.RECORD_TYPE && to == NodeType.LAYOUT) {
            return new EdgeResolution(EdgeType.ASSOCIATED_WITH, DependencyStrength.SOFT_DEPENDENCY);
        }

        if (from == NodeType.FLEXI_PAGE && (to == NodeType.LWC || to == NodeType.AURA_COMPONENT)) {
            return new EdgeResolution(EdgeType.CONTAINS, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.FLEXI_PAGE && isObjectNode(to)) {
            return new EdgeResolution(EdgeType.ASSOCIATED_WITH, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.CUSTOM_TAB && (to == NodeType.LWC || to == NodeType.AURA_COMPONENT || to == NodeType.FLEXI_PAGE)) {
            return new EdgeResolution(EdgeType.HOSTS, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.CUSTOM_APPLICATION && (to == NodeType.CUSTOM_TAB || to == NodeType.FLEXI_PAGE || to == NodeType.WEB_LINK)) {
            return new EdgeResolution(EdgeType.CONTAINS, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.WEB_LINK && isObjectNode(to)) {
            return new EdgeResolution(EdgeType.ASSOCIATED_WITH, DependencyStrength.SOFT_DEPENDENCY);
        }

        if (from == NodeType.CUSTOM_METADATA_RECORD && to == NodeType.CUSTOM_METADATA_TYPE) {
            return new EdgeResolution(EdgeType.BELONGS_TO, DependencyStrength.HARD_DEPENDENCY);
        }
        if (from == NodeType.CUSTOM_METADATA_TYPE && to == NodeType.CUSTOM_METADATA_RECORD) {
            return new EdgeResolution(EdgeType.CONTAINS, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.CUSTOM_SETTINGS && isObjectOrField(to)) {
            return new EdgeResolution(EdgeType.CONFIGURES, DependencyStrength.CONDITIONAL_DEPENDENCY);
        }
        if (from == NodeType.CUSTOM_METADATA_TYPE && isObjectOrField(to)) {
            return new EdgeResolution(EdgeType.CONFIGURES, DependencyStrength.CONDITIONAL_DEPENDENCY);
        }

        if (from == NodeType.NAMED_CREDENTIAL && to == NodeType.REMOTE_SITE_SETTING) {
            return new EdgeResolution(EdgeType.ASSOCIATED_WITH, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.REMOTE_SITE_SETTING && to == NodeType.NAMED_CREDENTIAL) {
            return new EdgeResolution(EdgeType.ASSOCIATED_WITH, DependencyStrength.SOFT_DEPENDENCY);
        }

        if (from == NodeType.PERMISSION_SET && isAccessTarget(to)) {
            return new EdgeResolution(EdgeType.ENABLES, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.PROFILE && isAccessTarget(to)) {
            return new EdgeResolution(EdgeType.ENABLES, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.PERMISSION_SET_GROUP && to == NodeType.PERMISSION_SET) {
            return new EdgeResolution(EdgeType.CONTAINS, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.SHARING_RULE && isObjectNode(to)) {
            return new EdgeResolution(EdgeType.EXPOSES, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.QUEUE && isObjectNode(to)) {
            return new EdgeResolution(EdgeType.ASSOCIATED_WITH, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.ROLE && to == NodeType.ROLE) {
            return new EdgeResolution(EdgeType.EXTENDS, DependencyStrength.SOFT_DEPENDENCY);
        }

        if (from == NodeType.EMAIL_TEMPLATE && isObjectNode(to)) {
            return new EdgeResolution(EdgeType.USES, DependencyStrength.SOFT_DEPENDENCY);
        }
        if (from == NodeType.STATIC_RESOURCE && (to == NodeType.LWC || to == NodeType.AURA_COMPONENT)) {
            return new EdgeResolution(EdgeType.EXPOSES, DependencyStrength.SOFT_DEPENDENCY);
        }

        return new EdgeResolution(EdgeType.DEPENDS_ON, DependencyStrength.UNKNOWN);
    }

    private static boolean isObjectNode(NodeType nodeType) {
        return nodeType == NodeType.CUSTOM_OBJECT || nodeType == NodeType.STANDARD_OBJECT;
    }

    private static boolean isObjectOrField(NodeType nodeType) {
        return isObjectNode(nodeType) || nodeType == NodeType.CUSTOM_FIELD;
    }

    private static boolean isConfigNode(NodeType nodeType) {
        return nodeType == NodeType.CUSTOM_METADATA_TYPE
                || nodeType == NodeType.CUSTOM_METADATA_RECORD
                || nodeType == NodeType.CUSTOM_SETTINGS;
    }

    private static boolean isIntegrationNode(NodeType nodeType) {
        return nodeType == NodeType.NAMED_CREDENTIAL
                || nodeType == NodeType.REMOTE_SITE_SETTING;
    }

    private static boolean isUiResource(NodeType nodeType) {
        return nodeType == NodeType.STATIC_RESOURCE
                || nodeType == NodeType.EMAIL_TEMPLATE
                || nodeType == NodeType.FLEXI_PAGE
                || nodeType == NodeType.CUSTOM_TAB
                || nodeType == NodeType.WEB_LINK
                || nodeType == NodeType.CUSTOM_APPLICATION;
    }

    private static boolean isAccessTarget(NodeType nodeType) {
        return isObjectOrField(nodeType)
                || nodeType == NodeType.APEX_CLASS
                || nodeType == NodeType.APEX_PAGE
                || nodeType == NodeType.LWC
                || nodeType == NodeType.AURA_COMPONENT
                || nodeType == NodeType.FLEXI_PAGE
                || nodeType == NodeType.CUSTOM_TAB
                || nodeType == NodeType.CUSTOM_PERMISSION
                || nodeType == NodeType.CUSTOM_APPLICATION;
    }
}
