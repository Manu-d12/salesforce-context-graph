package org.autorabit.salesforcecontextgraph.api.controller;

import lombok.AllArgsConstructor;
import org.autorabit.salesforcecontextgraph.service.SyncOrgMetadataService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/sync-org-metadata")
public class SyncOrgMetadataController {

    private final SyncOrgMetadataService syncOrgMetadataService;

    @PostMapping("/{sfOrgId}")
    public String syncOrgMetadata(
            @PathVariable String sfOrgId
    ) {
        syncOrgMetadataService.sync(sfOrgId);
        System.out.println("Synchronization started!! Controller");
        return "Synchronization started!!";
    }

}
