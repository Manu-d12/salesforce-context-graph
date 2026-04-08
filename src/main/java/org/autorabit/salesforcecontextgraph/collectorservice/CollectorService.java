package org.autorabit.salesforcecontextgraph.collectorservice;

import java.util.List;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;

public interface CollectorService {
    List<GraphEdge> buildRelativeGraphEdges();
    void persistRelativeGraphEdges();
}
