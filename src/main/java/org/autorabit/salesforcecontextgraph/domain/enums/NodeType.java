package org.autorabit.salesforcecontextgraph.domain.enums;

public enum NodeType {
    CUSTOM_OBJECT("CustomObject"),
    STANDARD_OBJECT("StandardEntity"),
    CUSTOM_FIELD("CustomField"),
    PERMISSION_SET("PermissionSet"),
    PERMISSION_SET_GROUP("PermissionSetGroup"),
    PROFILE("Profile"),
    ROLE("Role"),
    APEX_CLASS("ApexClass"),
    APEX_PAGE("ApexPage"),
    APEX_TRIGGER("ApexTrigger"),
    LWC("LightningComponentBundle"),
    AURA_COMPONENT("AuraDefinitionBundle"),
    FLOW("FlowDefinition"),
    VALIDATION_RULE("ValidationRule"),
    LAYOUT("Layout"),
    RECORD_TYPE("RecordType"),
    CUSTOM_METADATA_TYPE("CustomMetadata"),
    CUSTOM_METADATA_RECORD("CustomMetadataRecord"),
    CUSTOM_SETTINGS("CustomSetting"),
    NAMED_CREDENTIAL("NamedCredential"),
    REMOTE_SITE_SETTING("RemoteSiteSetting"),
    SHARING_RULE("SharingRule"),
    QUEUE("Queue"),
    STATIC_RESOURCE("StaticResource"),
    CONTENT_ASSET("ContentAsset"),
    EMAIL_TEMPLATE("EmailTemplate"),
    CUSTOM_TAB("CustomTab"),
    FLEXI_PAGE("FlexiPage"),
    WEB_LINK("WebLink"),
    CUSTOM_LABEL("CustomLabel"),
    CUSTOM_PERMISSION("CustomPermission"),
    CUSTOM_APPLICATION("CustomApplication");

    private final String metadatatype;

    NodeType(String metadatatype) {
        this.metadatatype = metadatatype;
    }

    public String getMetadatatype() {
        return metadatatype;
    }

    public static NodeType getNodeType(String metadatatype) {
        for(NodeType nodeType : NodeType.values()) {
            if(nodeType.getMetadatatype().equals(metadatatype)) {
                return nodeType;
            }
        }
        return null;
    }
}
