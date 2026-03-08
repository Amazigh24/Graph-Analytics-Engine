package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Edge;

import java.util.*;

public class DfsAlgorithm implements GraphAlgorithm<List<String>> {

    private final String sourceId;

    public DfsAlgorithm(String sourceId) {
        this.sourceId = sourceId;
    }

    @Override
    public List<String> execute(AdjacencyListGraph graph) {
        List<String> result = new ArrayList<>();
        if (graph.getNode(sourceId).isEmpty()) {
            return result;
        }

        Set<String> visited = new LinkedHashSet<>();
        dfsRecursive(graph, sourceId, visited);

        result.addAll(visited);
        return result;
    }

    private void dfsRecursive(AdjacencyListGraph graph, String current, Set<String> visited) {
        if (!visited.add(current))
            return;

        for (Edge edge : graph.getEdges(current)) {
            dfsRecursive(graph, edge.getTarget().getId(), visited);
        }
    }
}
