package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Edge;

import java.util.*;

public class BfsAlgorithm implements GraphAlgorithm<List<String>> {

    private final String sourceId;

    public BfsAlgorithm(String sourceId) {
        this.sourceId = sourceId;
    }

    @Override
    public List<String> execute(AdjacencyListGraph graph) {
        List<String> result = new ArrayList<>();
        if (graph.getNode(sourceId).isEmpty()) {
            return result;
        }

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(sourceId);
        visited.add(sourceId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);

            for (Edge edge : graph.getEdges(current)) {
                String neighbor = edge.getTarget().getId();
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }
}
