package org.autorabit.salesforcecontextgraph.collectorservice;

import java.util.List;

import org.autorabit.salesforcecontextgraph.api.request.SfOrgSyncRequestDto;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;

public interface CollectorService {
    List<GraphEdge> buildRelativeGraphEdges();
    default void persistRelativeGraphEdges(SfOrgSyncRequestDto requestDto) {
        persistRelativeGraphEdges(requestDto, null);
    }

    default void persistRelativeGraphEdges(SfOrgSyncRequestDto requestDto, SalesforceSession session) {
        throw new UnsupportedOperationException("persistRelativeGraphEdges is not implemented");
    }
}
