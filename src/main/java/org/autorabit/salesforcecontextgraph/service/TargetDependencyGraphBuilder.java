package org.autorabit.salesforcecontextgraph.service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.autorabit.salesforcecontextgraph.api.request.AnalysisRequestDto;
import org.autorabit.salesforcecontextgraph.domain.enums.NodeType;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.domain.model.RuntimeGraph;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;
import org.autorabit.salesforcecontextgraph.integration.salesforce.ToolingApiClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class TargetDependencyGraphBuilder {

    private final MetadataReaderService metadataReaderService;
    private final GraphBuilderAgent graphBuilderAgent;
    private final ToolingApiClient toolingApiClient;

    public RuntimeGraph buildGraph(AnalysisRequestDto requestDto) {
        return buildGraph(requestDto, null);
    }

    public RuntimeGraph buildGraph(AnalysisRequestDto requestDto, SalesforceSession session) {
        List<GraphEdge> edges = new ArrayList<>();
        buildGraphRecursively(requestDto.targetNodes(), edges, session);
        System.out.println(edges);
        return graphBuilderAgent.build(edges);
    }

    private void buildGraphRecursively (
            Map<NodeType, List<String>> nodeTypeListMap,
            List<GraphEdge> edges,
            SalesforceSession session
    ) {
        if(nodeTypeListMap.isEmpty()) return;
        String soql = soqlBuilder(nodeTypeListMap);
        Map<NodeType, List<String>> nextTargetNodes = new HashMap<>();

        List<Map<String, Object>> dependencies = toolingApiClient.query(soql, session);

        for (Map<String, Object> row : dependencies) {
            String metadataId = stringValue(row, "MetadataComponentId");
            String metadataName = stringValue(row, "MetadataComponentName");
            String metadataType = stringValue(row, "MetadataComponentType");
            String refId = stringValue(row, "RefMetadataComponentId");
            String refName = stringValue(row, "RefMetadataComponentName");
            String refType = stringValue(row, "RefMetadataComponentType");

            if (metadataId == null || metadataName == null || metadataType == null
                    || refId == null || refName == null || refType == null) {
                continue;
            }

            String parentMetadataType = NodeType.getNodeType(metadataType) != null
                    ? NodeType.getNodeType(metadataType).toString()
                    : metadataType;

            String childMetadataType = NodeType.getNodeType(refType) != null
                    ? Objects.requireNonNull(NodeType.getNodeType(refType)).toString()
                    : refType;

            edges.add(new GraphEdge(
                    GraphNode.buildGraphNode(metadataId, parentMetadataType, metadataName),
                    GraphNode.buildGraphNode(refId, childMetadataType, refName),
                    EdgeResolverService.resolve(metadataType, refType).toString()
            ));

            NodeType refNodeType = NodeType.getNodeType(refType);
            if(nextTargetNodes.containsKey(refNodeType)) nextTargetNodes.get(refNodeType).add(refId);
            else nextTargetNodes.put(refNodeType, new ArrayList<>(List.of(refId)));
        }

        buildGraphRecursively(nextTargetNodes, edges, session);

    }

    private String stringValue(Map<String, Object> record, String key) {
        Object value = record.get(key);
        return value == null ? null : value.toString();
    }

    private String soqlBuilder(Map<NodeType, List<String>> metadataMap) {
        StringBuilder soql = new StringBuilder("SELECT MetadataComponentId, RefMetadataComponentId, MetadataComponentName, MetadataComponentType, RefMetadataComponentName, RefMetadataComponentType FROM MetadataComponentDependency WHERE ");
        boolean first = true;
        for(NodeType metadataType : metadataMap.keySet()) {
            List<String> metadataNames = metadataMap.get(metadataType);
            String orClause = " (MetadataComponentType = '" + metadataType.getMetadatatype() + "' AND MetadataComponentId IN ('" + String.join("','", metadataNames) + "'))";
            if(!first) {
                soql.append(" OR ");
            }
            first = false;
            soql.append(orClause);
        }
        return soql.toString();
    }

    @PostConstruct
    public void init() {
//        Map<NodeType, List<String>> metadataMap = new HashMap<>();
//
//        metadataMap.put(NodeType.APEX_CLASS, List.of("01pg5000004lPzxAAE"));
//        metadataMap.put(NodeType.LWC, List.of("DynamicTable", "HelloWorld", "DynamicRow", "GreetingLWC"));
//
//        AnalysisRequestDto requestDto = new AnalysisRequestDto(AnalysisType.DEPENDENCY, metadataMap);
//        String soql = soqlBuilder(metadataMap);
//        System.out.println(soql);
//        buildGraph(requestDto);
    }

}
