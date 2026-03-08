package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Edge;

import java.util.*;

public class ClosenessCentralityAlgorithm implements GraphAlgorithm<Map<String, Double>> {

    @Override
    public Map<String, Double> execute(AdjacencyListGraph graph) {
        Map<String, Double> centralityScores = new HashMap<>();
        long n = graph.getNodeCount();

        if (n <= 1) {
            graph.getNodes().forEach(node -> centralityScores.put(node.getId(), 0.0));
            return centralityScores;
        }

        for (var sourceNode : graph.getNodes()) {
            String sourceId = sourceNode.getId();
            double sumDistances = computeSumShortestPaths(graph, sourceId);

            if (sumDistances == Double.POSITIVE_INFINITY || sumDistances == 0) {
                centralityScores.put(sourceId, 0.0);
            } else {
                centralityScores.put(sourceId, (n - 1) / sumDistances);
            }
        }

        return centralityScores;
    }

    private double computeSumShortestPaths(AdjacencyListGraph graph, String sourceId) {
        Map<String, Double> distances = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));

        graph.getNodes().forEach(node -> distances.put(node.getId(), Double.POSITIVE_INFINITY));
        distances.put(sourceId, 0.0);
        pq.add(new NodeDistance(sourceId, 0.0));

        Set<String> visited = new HashSet<>();

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String currentId = current.nodeId;

            if (!visited.add(currentId)) {
                continue;
            }

            for (Edge edge : graph.getEdges(currentId)) {
                String neighborId = edge.getTarget().getId();
                if (visited.contains(neighborId)) {
                    continue;
                }

                double newDist = distances.get(currentId) + edge.getWeight();
                if (newDist < distances.get(neighborId)) {
                    distances.put(neighborId, newDist);
                    pq.add(new NodeDistance(neighborId, newDist));
                }
            }
        }

        double sum = 0;
        for (Double dist : distances.values()) {
            if (dist == Double.POSITIVE_INFINITY) {
                return Double.POSITIVE_INFINITY;
            }
            sum += dist;
        }
        return sum;
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
