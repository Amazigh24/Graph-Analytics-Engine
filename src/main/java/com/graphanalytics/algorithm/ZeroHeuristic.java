package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;

public class ZeroHeuristic implements AStarHeuristic {

    @Override
    public double calculate(AdjacencyListGraph graph, String sourceId, String targetId) {
        // Returns 0.0, making A* behave exactly like Dijkstra's algorithm.
        return 0.0;
    }
}
