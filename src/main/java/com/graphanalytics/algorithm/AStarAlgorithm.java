package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Edge;

import java.util.*;

public class AStarAlgorithm implements GraphAlgorithm<PathResult> {

    private final String sourceId;
    private final String targetId;

    public AStarAlgorithm(String sourceId, String targetId) {
        this.sourceId = sourceId;
        this.targetId = targetId;
    }

    @Override
    public PathResult execute(AdjacencyListGraph graph) {
        if (graph.getNode(sourceId).isEmpty() || graph.getNode(targetId).isEmpty()) {
            return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY);
        }

        Map<String, Double> gScore = new HashMap<>(); // Cost from start
        Map<String, Double> fScore = new HashMap<>(); // Estimated total cost
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<NodeDistance> openSet = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));

        graph.getNodes().forEach(node -> {
            gScore.put(node.getId(), Double.POSITIVE_INFINITY);
            fScore.put(node.getId(), Double.POSITIVE_INFINITY);
        });

        gScore.put(sourceId, 0.0);
        fScore.put(sourceId, heuristic(graph, sourceId, targetId));
        openSet.add(new NodeDistance(sourceId, fScore.get(sourceId)));

        Set<String> closedSet = new HashSet<>();

        while (!openSet.isEmpty()) {
            NodeDistance current = openSet.poll();
            String currentId = current.nodeId;

            if (currentId.equals(targetId)) {
                return reconstructPath(previous, targetId, gScore.get(targetId));
            }

            if (!closedSet.add(currentId)) {
                continue;
            }

            for (Edge edge : graph.getEdges(currentId)) {
                String neighborId = edge.getTarget().getId();
                if (closedSet.contains(neighborId)) {
                    continue;
                }

                double tentativeGScore = gScore.get(currentId) + edge.getWeight();
                if (tentativeGScore < gScore.get(neighborId)) {
                    previous.put(neighborId, currentId);
                    gScore.put(neighborId, tentativeGScore);
                    double f = tentativeGScore + heuristic(graph, neighborId, targetId);
                    fScore.put(neighborId, f);
                    openSet.add(new NodeDistance(neighborId, f));
                }
            }
        }

        return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY);
    }

    private double heuristic(AdjacencyListGraph graph, String nodeId, String targetId) {
        // Implementation of heuristic.
        // For spatial graphs, this would be Euclidean or Manhattan distance.
        // Assuming coordinates are stored in node properties.
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

        // Fallback or generic heuristic: returns 0 makes it behave like Dijkstra
        return 0.0;
    }

    private PathResult reconstructPath(Map<String, String> previous, String current, double totalCost) {
        List<String> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }
        Collections.reverse(path);
        return new PathResult(path, totalCost);
    }

    private static class NodeDistance {
        String nodeId;
        double distance;

        public NodeDistance(String nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }
}
