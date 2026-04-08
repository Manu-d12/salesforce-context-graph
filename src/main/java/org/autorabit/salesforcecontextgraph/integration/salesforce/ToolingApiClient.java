package org.autorabit.salesforcecontextgraph.integration.salesforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.autorabit.salesforcecontextgraph.config.SalesforceIntegrationProperties;
import org.springframework.stereotype.Component;

@Component
public class ToolingApiClient {

    private final SalesforceOAuthService oAuthService;
    private final SalesforceIntegrationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ToolingApiClient(
            SalesforceOAuthService oAuthService,
            SalesforceIntegrationProperties properties,
            ObjectMapper objectMapper
    ) {
        this.oAuthService = oAuthService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public List<Map<String, Object>> query(String soql) {
        return query(soql, null);
    }

    public List<Map<String, Object>> query(String soql, SalesforceSession session) {
        SalesforceSession activeSession = session == null ? oAuthService.authenticate() : session;
        String endpoint = trimTrailingSlash(activeSession.instanceUrl())
                + "/services/data/"
                + properties.getApiVersion()
                + "/tooling/query?q="
                + URLEncoder.encode(soql, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + activeSession.accessToken())
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Tooling API query failed: " + response.body());
            }
            JsonNode payload = objectMapper.readTree(response.body());
            List<Map<String, Object>> records = new ArrayList<>();
            for (JsonNode recordNode : payload.path("records")) {
                records.add(objectMapper.convertValue(recordNode, Map.class));
            }
            return records;
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tooling API query failed", ex);
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
