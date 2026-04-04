package org.autorabit.salesforcecontextgraph.api.controller;

import java.util.List;
import java.util.Map;
import org.autorabit.salesforcecontextgraph.api.request.ToolingQueryRequestDto;
import org.autorabit.salesforcecontextgraph.api.response.SalesforceSessionResponse;
import org.autorabit.salesforcecontextgraph.config.SalesforceIntegrationProperties;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceOAuthService;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;
import org.autorabit.salesforcecontextgraph.integration.salesforce.ToolingApiClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salesforce")
public class SalesforceController {

    private final SalesforceOAuthService oAuthService;
    private final ToolingApiClient toolingApiClient;
    private final SalesforceIntegrationProperties properties;

    public SalesforceController(
            SalesforceOAuthService oAuthService,
            ToolingApiClient toolingApiClient,
            SalesforceIntegrationProperties properties
    ) {
        this.oAuthService = oAuthService;
        this.toolingApiClient = toolingApiClient;
        this.properties = properties;
    }

    @GetMapping("/session")
    public SalesforceSessionResponse validateSession() {
        SalesforceSession session = oAuthService.authenticate();
        return new SalesforceSessionResponse(session.instanceUrl(), session.idUrl(), properties.getApiVersion());
    }

    @PostMapping("/tooling/query")
    public List<Map<String, Object>> runToolingQuery(@RequestBody ToolingQueryRequestDto request) {
        if (request == null || request.soql() == null || request.soql().isBlank()) {
            throw new IllegalArgumentException("soql is required");
        }
        return toolingApiClient.query(request.soql().trim());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleErrors(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }
}
