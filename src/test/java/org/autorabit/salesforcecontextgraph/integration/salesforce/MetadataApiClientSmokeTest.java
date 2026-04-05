package org.autorabit.salesforcecontextgraph.integration.salesforce;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.autorabit.salesforcecontextgraph.config.SalesforceIntegrationProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MetadataApiClientSmokeTest {

    @Test
    void listsCustomObjectMetadata() {
        SalesforceIntegrationProperties properties = new SalesforceIntegrationProperties();
        properties.setLoginUrl(readConfig("SALESFORCE_LOGIN_URL",
                "https://orgfarm-f90b1a99a1-dev-ed.develop.my.salesforce.com"));
        properties.setClientId(readConfig("SALESFORCE_CLIENT_ID",
                "3MVG97L7PWbPq6UwSwyY8vtcGcgY7WXWMDcXwg4dWVnai_JVPsP.vs7ppCYMpNuu8HKl01_h6NAlmYIvXQdhP"));
        properties.setClientSecret(readConfig("SALESFORCE_CLIENT_SECRET",
                "235F28D4E2BF8B25002659983F0A1C0C184E16982F7099086065E5ABCB02D3B8"));
        properties.setApiVersion(readConfig("SALESFORCE_API_VERSION", "v65.0"));

        SalesforceOAuthService oAuthService = new SalesforceOAuthService(properties, new ObjectMapper());
        MetadataApiClient metadataApiClient = new MetadataApiClient(oAuthService, properties);

        List<String> metadataNames = metadataApiClient.listMetadataFullNames("CustomObject");
        metadataNames.stream().limit(20).forEach(System.out::println);

        assertFalse(metadataNames.isEmpty(), "Expected Metadata API to return at least one CustomObject entry");
    }

    private String readConfig(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
