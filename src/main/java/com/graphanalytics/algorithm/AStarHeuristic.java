package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;

public interface AStarHeuristic {
    double calculate(AdjacencyListGraph graph, String sourceId, String targetId);
}
