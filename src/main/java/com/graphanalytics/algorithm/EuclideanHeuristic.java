package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;

public class EuclideanHeuristic implements AStarHeuristic {

    @Override
    public double calculate(AdjacencyListGraph graph, String nodeId, String targetId) {
        var nodeOpt = graph.getNode(nodeId);
        var targetOpt = graph.getNode(targetId);

        if (nodeOpt.isPresent() && targetOpt.isPresent()) {
            var node = nodeOpt.get();
            var target = targetOpt.get();

            Object nx = node.getProperty("x");
            Object ny = node.getProperty("y");
            Object tx = target.getProperty("x");
            Object ty = target.getProperty("y");

            if (nx instanceof Number && ny instanceof Number && tx instanceof Number && ty instanceof Number) {
                double dx = ((Number) nx).doubleValue() - ((Number) tx).doubleValue();
                double dy = ((Number) ny).doubleValue() - ((Number) ty).doubleValue();
                return Math.sqrt(dx * dx + dy * dy);
            }
        }

        return 0.0;
    }
}
