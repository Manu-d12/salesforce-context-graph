package org.autorabit.salesforcecontextgraph.integration.salesforce;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.config.SalesforceIntegrationProperties;
import org.springframework.stereotype.Service;

@Service
public class SalesforceOAuthService {

    private final SalesforceIntegrationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SalesforceOAuthService(SalesforceIntegrationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public SalesforceSession authenticate() {
        return authenticate(
                properties.getLoginUrl(),
                properties.getClientId(),
                properties.getClientSecret()
        );
    }

    public SalesforceSession authenticate(SfOrgSyncRequestDto requestDto) {
        if (!hasRequestCredentials(requestDto)) {
            return authenticate();
        }
        return authenticate(
                requestDto.loginUrl(),
                requestDto.clientId(),
                requestDto.clientSecret()
        );
    }

    private SalesforceSession authenticate(String loginUrl, String clientId, String clientSecret) {
        String body = buildRequestBody(clientId, clientSecret);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(requiredValue(loginUrl, "salesforce.login-url")) + "/services/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Salesforce authentication failed: " + response.body());
            }

            JsonNode payload = objectMapper.readTree(response.body());
            return new SalesforceSession(
                    required(payload, "access_token"),
                    required(payload, "instance_url"),
                    payload.path("id").asText(null)
            );
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Salesforce authentication failed", ex);
        }
    }

    private String buildRequestBody(String clientId, String clientSecret) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "client_credentials");
        form.put("client_id", requiredValue(clientId, "salesforce.client-id"));
        form.put("client_secret", requiredValue(clientSecret, "salesforce.client-secret"));

        return form.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String required(JsonNode payload, String field) throws JsonProcessingException {
        if (!payload.hasNonNull(field)) {
            throw new JsonProcessingException("Missing field in Salesforce auth response: " + field) {
            };
        }
        return payload.get(field).asText();
    }

    private String requiredValue(String value, String propertyName) {
        if (!hasText(value)) {
            throw new IllegalStateException("Missing Salesforce configuration: " + propertyName);
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasRequestCredentials(SfOrgSyncRequestDto requestDto) {
        return requestDto != null
                && hasText(requestDto.loginUrl())
                && hasText(requestDto.clientId())
                && hasText(requestDto.clientSecret());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
