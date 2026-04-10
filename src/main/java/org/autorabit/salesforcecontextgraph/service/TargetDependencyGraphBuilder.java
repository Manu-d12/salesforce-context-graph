package org.autorabit.salesforcecontextgraph.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import org.autorabit.salesforcecontextgraph.api.request.AnalysisRequestDto;
import org.autorabit.salesforcecontextgraph.db_entities.MetadataDependency;
import org.autorabit.salesforcecontextgraph.domain.model.GraphEdge;
import org.autorabit.salesforcecontextgraph.domain.model.GraphNode;
import org.autorabit.salesforcecontextgraph.domain.model.RuntimeGraph;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;
import org.autorabit.salesforcecontextgraph.repository.MetadataDependencyRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class TargetDependencyGraphBuilder {

    @PersistenceContext
    private EntityManager entityManager;
    private final MetadataApiClient metadataApiClient;
    private final MetadataDependencyRepository metadataDependencyRepository;
    private final GraphBuilderAgent graphBuilderAgent;

    public RuntimeGraph buildGraph(AnalysisRequestDto requestDto, String sfOrgId) {
        List<GraphEdge> edges = new ArrayList<>();
        Map<String, Set<String>> targetNodes = normalizeTargets(requestDto);
        buildGraphRecursively(targetNodes, edges, sfOrgId, new HashSet<>(), new HashSet<>());
        return graphBuilderAgent.build(edges);
    }

    private void buildGraphRecursively(
            Map<String, Set<String>> nodeTypeListMap,
            List<GraphEdge> edges,
            String sfOrgId,
            Set<String> visitedNodes,
            Set<String> visitedEdges
    ) {
        if (nodeTypeListMap == null || nodeTypeListMap.isEmpty()) {
            return;
        }

        Map<String, Set<String>> unresolvedTargets = filterUnvisitedTargets(nodeTypeListMap, visitedNodes);
        if (unresolvedTargets.isEmpty()) {
            return;
        }

        List<MetadataDependency> dependencies = entityManager
                .createNativeQuery(sqlBuilder(unresolvedTargets, sfOrgId), MetadataDependency.class)
                .getResultList();

        Map<String, Set<String>> nextNodeTypeListMap = new LinkedHashMap<>();
        for (MetadataDependency dependency : dependencies) {
            GraphEdge edge = toGraphEdge(dependency);
            if (visitedEdges.add(edgeKey(edge))) {
                edges.add(edge);
            }

            if (dependency.getRefMetadataType() == null || dependency.getRefMetadataName() == null) {
                continue;
            }

            nextNodeTypeListMap
                    .computeIfAbsent(dependency.getRefMetadataType(), ignored -> new LinkedHashSet<>())
                    .add(dependency.getRefMetadataName());
        }

        if (!nextNodeTypeListMap.isEmpty()) {
            buildGraphRecursively(nextNodeTypeListMap, edges, sfOrgId, visitedNodes, visitedEdges);
        }
    }

    private Map<String, Set<String>> filterUnvisitedTargets(
            Map<String, Set<String>> nodeTypeListMap,
            Set<String> visitedNodes
    ) {
        Map<String, Set<String>> unresolvedTargets = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : nodeTypeListMap.entrySet()) {
            String metadataType = entry.getKey();
            Set<String> metadataNames = entry.getValue();
            if (metadataType == null || metadataType.isBlank() || metadataNames == null || metadataNames.isEmpty()) {
                continue;
            }

            Set<String> unresolvedNames = metadataNames.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .filter(value -> visitedNodes.add(nodeKey(metadataType, value)))
                    .collect(LinkedHashSet::new, Set::add, Set::addAll);

            if (!unresolvedNames.isEmpty()) {
                unresolvedTargets.put(metadataType, unresolvedNames);
            }
        }
        return unresolvedTargets;
    }

    private Map<String, Set<String>> normalizeTargets(AnalysisRequestDto requestDto) {
        Map<String, Set<String>> normalizedTargets = new LinkedHashMap<>();
        if (requestDto == null || requestDto.targetNodes() == null) {
            return normalizedTargets;
        }

        requestDto.targetNodes().forEach((nodeType, values) -> {
            if (nodeType == null || values == null || values.isEmpty()) {
                return;
            }

            Set<String> normalizedValues = values.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .collect(LinkedHashSet::new, Set::add, Set::addAll);

            if (!normalizedValues.isEmpty()) {
                normalizedTargets.put(nodeType.toString(), normalizedValues);
            }
        });
        return normalizedTargets;
    }

    private GraphEdge toGraphEdge(MetadataDependency dependency) {
        GraphNode fromNode = GraphNode.buildGraphNode(
                dependency.getMetadataName(),
                dependency.getMetadataType()
        );
        GraphNode toNode = GraphNode.buildGraphNode(
                dependency.getRefMetadataName(),
                dependency.getRefMetadataType()
        );
        return new GraphEdge(fromNode, toNode, dependency.getEdgeType());
    }

    private String nodeKey(String metadataType, String metadataName) {
        return metadataType + "|" + metadataName;
    }

    private String edgeKey(GraphEdge edge) {
        return edge.fromNode().id() + "|" + edge.toNode().id() + "|" + edge.type();
    }

    private String sqlBuilder(Map<String, Set<String>> metadataMap, String sfOrgId) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, org_id, edge_source, metadata_type, metadata_name, metadata_label,
                       ref_metadata_type, ref_metadata_name, ref_metadata_label, edge_type
                FROM metadata_dependency
                WHERE org_id = '""");
        sql.append(escapeSql(sfOrgId)).append("""
                '
                  AND (
                """);

        boolean firstType = true;
        for (Map.Entry<String, Set<String>> entry : metadataMap.entrySet()) {
            if (!firstType) {
                sql.append(" OR ");
            }
            firstType = false;

            if (entry.getValue().contains("$All")) {
                sql.append("(metadata_type = '").append(escapeSql(entry.getKey())).append("')");
                continue;
            }

            sql.append("(metadata_type = '")
                    .append(escapeSql(entry.getKey()))
                    .append("' AND metadata_name IN (");

            boolean firstName = true;
            for (String metadataName : entry.getValue()) {

                if (!firstName) {
                    sql.append(", ");
                }
                firstName = false;
                sql.append("'").append(escapeSql(metadataName)).append("'");
            }
            sql.append("))");
        }

        sql.append(")");
        return sql.toString();
    }

    private String escapeSql(String value) {
        return value.replace("'", "''");
    }

}
