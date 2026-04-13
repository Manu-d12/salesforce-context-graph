package org.autorabit.salesforcecontextgraph.api.controller;

import lombok.AllArgsConstructor;
import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceOAuthService;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;
import org.autorabit.salesforcecontextgraph.service.SyncJobTransactionService;
import org.autorabit.salesforcecontextgraph.service.SyncOrgMetadataService;
import org.autorabit.salesforcecontextgraph.utils.Helper;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/sync-org-metadata")
public class SyncOrgMetadataController {

    private final SyncOrgMetadataService syncOrgMetadataService;
    private final SyncJobTransactionService syncJobTransactionService;
    private final MetadataApiClient metadataApiClient;
    private final SalesforceOAuthService salesforceOAuthService;

    @PostMapping
    public String syncOrgMetadata(
            @RequestBody  SfOrgSyncRequestDto requestDto
    ) {
        SalesforceSession session = salesforceOAuthService.authenticate(requestDto);
        String orgId = Helper.resolveOrgId(metadataApiClient, session);
        boolean syncInProgress = syncJobTransactionService.isSyncInProgress(orgId);
        if(syncInProgress) return "Already in progress.";
        syncOrgMetadataService.sync(requestDto, session, orgId);
        System.out.println("Synchronization started!! Controller");
        return "Synchronization started!!";
    }

    @GetMapping("/jobStatus/{orgId}")
    public String getLastSyncInJobStatus(
            @PathVariable String orgId
    ) {
        return syncOrgMetadataService.getLastSyncInJobStatus(orgId);
    }

}
