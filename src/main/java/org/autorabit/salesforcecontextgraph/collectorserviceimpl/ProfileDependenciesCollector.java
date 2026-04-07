package org.autorabit.salesforcecontextgraph.collectorserviceimpl;

import org.autorabit.salesforcecontextgraph.collectorservice.CollectorService;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;

import java.util.List;

public class ProfileDependenciesCollector implements CollectorService {

    @Override
    public List<GraphEdge> buildRelativeGraphEdges() {
        return List.of();
    }
}
