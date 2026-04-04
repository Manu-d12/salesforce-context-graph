package org.autorabit.salesforcecontextgraph.utility;


import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;

public class Helper {

    public static String toMetadataTypes(NodeType nodeType) {
        if (nodeType == null) return "";
        return switch (nodeType) {
            case CUSTOM_OBJECT -> "CustomObject";
            case STANDARD_OBJECT -> "StandardEntity";
            case FIELD -> "CustomField,StandardField";
            case PERMISSION_SET -> "PermissionSet";
            case PROFILE -> "Profile";
            case ROLE -> "UserRole";
            case PERMISSION_SET_GROUP -> "PermissionSetGroup";
            case APEX_CLASS -> "ApexClass";
            case APEX_TRIGGER -> "ApexTrigger";
            case LWC -> "LightningComponentBundle";
            case AURA_COMPONENT -> "AuraDefinitionBundle";
            case FLOW -> "Flow,FlowDefinition";
            case VALIDATION_RULE -> "ValidationRule";
            case LAYOUT -> "Layout";
            case FLEXIPAGE -> "FlexiPage";
            case CUSTOM_TAB -> "CustomTab";
            case RECORD_TYPE -> "RecordType";
            case CUSTOM_METADATA_TYPE -> "CustomMetadata";
            case CUSTOM_METADATA_RECORD -> "CustomMetadataRecord";
            case CUSTOM_SETTINGS -> "CustomSetting";
            case NAMED_CREDENTIAL -> "NamedCredential";
            case REMOTE_SITE_SETTING -> "RemoteSiteSetting";
            case SHARING_RULE -> "SharingRule";
            case QUEUE -> "Queue";
            case STATIC_RESOURCE -> "StaticResource";
            case EMAIL_TEMPLATE -> "EmailTemplate";
            case CUSTOM_APPLICATION -> "CustomApplication";
            default -> "";
        };
    }
}
