package org.autorabit.salesforcecontextgraph.api.controller;

import lombok.AllArgsConstructor;
import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.service.SyncOrgMetadataService;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/sync-org-metadata")
public class SyncOrgMetadataController {

    private final SyncOrgMetadataService syncOrgMetadataService;

    @PostMapping
    public String syncOrgMetadata(
            @RequestBody  SfOrgSyncRequestDto requestDto
    ) {
        syncOrgMetadataService.sync(requestDto);
        System.out.println("Synchronization started!! Controller");
        return "Synchronization started!!";
    }

}
