package org.autorabit.salesforcecontextgraph.integration.salesforce;

import com.sforce.soap.metadata.*;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        if (metadataApiNames == null || metadataApiNames.isEmpty()) {
            return List.of();
        }
        try {
            MetadataConnection metadataConnection = createConnection();
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
        try {
            MetadataConnection metadataConnection = createConnection();
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
        try {
            MetadataConnection metadataConnection = createConnection();
            return metadataConnection.describeMetadata(Double.parseDouble(apiVersionNumber()));
        } catch (ConnectionException ex) {
            throw new IllegalStateException("Metadata API describeMetadata failed", ex);
        }
    }

    public List<CustomObject> readCustomObjects(List<String> objectNames) {
        try {
            MetadataConnection metadataConnection = createConnection();
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

    private MetadataConnection createConnection() throws ConnectionException {
        SalesforceSession session = oAuthService.authenticate();
        ConnectorConfig config = new ConnectorConfig();
        config.setSessionId(session.accessToken());
        config.setServiceEndpoint(trimTrailingSlash(session.instanceUrl()) + "/services/Soap/m/" + apiVersionNumber());
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
}
