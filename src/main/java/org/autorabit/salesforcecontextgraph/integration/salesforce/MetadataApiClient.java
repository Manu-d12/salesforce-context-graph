package org.autorabit.salesforcecontextgraph.integration.salesforce;

import com.sforce.soap.metadata.*;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.config.SalesforceIntegrationProperties;
import org.springframework.stereotype.Component;

@Component
public class MetadataApiClient {

    private final SalesforceOAuthService oAuthService;
    private final SalesforceIntegrationProperties properties;

    public MetadataApiClient(
            SalesforceOAuthService oAuthService,
            SalesforceIntegrationProperties properties
    ) {
        this.oAuthService = oAuthService;
        this.properties = properties;
    }

    public List<Metadata> getPermissionSetDescribe(List<String> metadataApiNames, String metadataType) {
        return getPermissionSetDescribe(metadataApiNames, metadataType, null);
    }

    public List<Metadata> getPermissionSetDescribe(
            List<String> metadataApiNames,
            String metadataType,
            SalesforceSession session
    ) {
        if (metadataApiNames == null || metadataApiNames.isEmpty()) {
            return List.of();
        }
        try {
            MetadataConnection metadataConnection = createConnection(session);
            List<Metadata> metaDataRecords = new ArrayList<>();
            for (int index = 0; index < metadataApiNames.size(); index += 10) {
                List<String> batch = metadataApiNames.subList(index, Math.min(index + 10, metadataApiNames.size()));
                ReadResult readResult = metadataConnection.readMetadata(metadataType, batch.toArray(String[]::new));
                if (readResult == null || readResult.getRecords() == null) {
                    continue;
                }
                metaDataRecords.addAll(Arrays.asList(readResult.getRecords()));
            }
            return metaDataRecords;
        } catch (ConnectionException ex) {
            throw new IllegalStateException("Metadata API readMetadata failed", ex);
        }
    }

    public List<String> listMetadataFullNames(String metadataType) {
        return listMetadataFullNames(metadataType, null);
    }

    public List<String> listMetadataFullNames(String metadataType, SalesforceSession session) {
        try {
            MetadataConnection metadataConnection = createConnection(session);
            ListMetadataQuery query = new ListMetadataQuery();
            query.setType(metadataType);

            double apiVersion = Double.parseDouble(apiVersionNumber());
            FileProperties[] properties = metadataConnection.listMetadata(
                    new ListMetadataQuery[]{query},
                    apiVersion
            );

            List<String> fullNames = new ArrayList<>();
            if (properties == null) {
                return fullNames;
            }

            for (FileProperties fileProperties : properties) {
                if (fileProperties != null && hasText(fileProperties.getFullName())) {
                    fullNames.add(fileProperties.getFullName());
                }
            }
            return fullNames;
        } catch (ConnectionException ex) {
            throw new IllegalStateException("Metadata API listMetadata failed", ex);
        }
    }

    public DescribeMetadataResult describeMetadata() {
        return describeMetadata(null);
    }

    public DescribeMetadataResult describeMetadata(SalesforceSession session) {
        try {
            MetadataConnection metadataConnection = createConnection(session);
            return metadataConnection.describeMetadata(Double.parseDouble(apiVersionNumber()));
        } catch (ConnectionException ex) {
            throw new IllegalStateException("Metadata API describeMetadata failed", ex);
        }
    }

    public List<CustomObject> readCustomObjects(List<String> objectNames) {
        return readCustomObjects(objectNames, null);
    }

    public List<CustomObject> readCustomObjects(List<String> objectNames, SalesforceSession session) {
        try {
            MetadataConnection metadataConnection = createConnection(session);
            List<CustomObject> customObjects = new ArrayList<>();
            for (int index = 0; index < objectNames.size(); index += 10) {
                List<String> batch = objectNames.subList(index, Math.min(index + 10, objectNames.size()));
                ReadResult readResult = metadataConnection.readMetadata("CustomObject", batch.toArray(String[]::new));
                if (readResult == null || readResult.getRecords() == null) {
                    continue;
                }
                for (Metadata record : readResult.getRecords()) {
                    if (record instanceof CustomObject customObject) {
                        customObjects.add(customObject);
                    }
                }
            }
            return customObjects;
        } catch (ConnectionException ex) {
            throw new IllegalStateException("Metadata API readMetadata failed", ex);
        }
    }

    public String resolveOrgId() {
        return resolveOrgId((SalesforceSession) null);
    }

    public String resolveOrgId(SfOrgSyncRequestDto requestDto) {
        return extractOrgId(oAuthService.authenticate(requestDto).idUrl());
    }

    public String resolveOrgId(SalesforceSession session) {
        SalesforceSession activeSession = session == null ? oAuthService.authenticate() : session;
        return extractOrgId(activeSession.idUrl());
    }

    private MetadataConnection createConnection(SalesforceSession session) throws ConnectionException {
        SalesforceSession activeSession = session == null ? oAuthService.authenticate() : session;
        ConnectorConfig config = new ConnectorConfig();
        config.setSessionId(activeSession.accessToken());
        config.setServiceEndpoint(trimTrailingSlash(activeSession.instanceUrl()) + "/services/Soap/m/" + apiVersionNumber());
        config.setManualLogin(true);
        return new MetadataConnection(config);
    }

    private String apiVersionNumber() {
        return properties.getApiVersion().startsWith("v")
                ? properties.getApiVersion().substring(1)
                : properties.getApiVersion();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String extractOrgId(String idUrl) {
        if (!hasText(idUrl)) {
            return null;
        }

        int separatorIndex = idUrl.lastIndexOf('/');
        if (separatorIndex < 0 || separatorIndex == idUrl.length() - 1) {
            return idUrl;
        }
        return idUrl.substring(separatorIndex + 1);
    }
}
