package com.graphanalytics.service;

import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Edge;

public class GraphValidator {

    public static void validateNoNegativeWeights(AdjacencyListGraph graph) {
        for (var node : graph.getNodes()) {
            for (Edge edge : graph.getEdges(node.getId())) {
                if (edge.getWeight() < 0) {
                    throw new IllegalArgumentException(
                            "Graph contains negative edge weights, which is not supported by this algorithm.");
                }
            }
        }
    }

    public static void validateNotEmpty(AdjacencyListGraph graph) {
        if (graph.getNodeCount() == 0) {
            throw new IllegalArgumentException("Graph is empty.");
        }
    }
}
