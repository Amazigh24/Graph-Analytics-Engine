package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Edge;

import java.util.*;

public class DijkstraAlgorithm implements GraphAlgorithm<PathResult> {

    private final String sourceId;
    private final String targetId;

    public DijkstraAlgorithm(String sourceId, String targetId) {
        this.sourceId = sourceId;
        this.targetId = targetId;
    }

    @Override
    public PathResult execute(AdjacencyListGraph graph) {
        if (graph.getNode(sourceId).isEmpty() || graph.getNode(targetId).isEmpty()) {
            return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY);
        }

        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));

        graph.getNodes().forEach(node -> distances.put(node.getId(), Double.POSITIVE_INFINITY));
        distances.put(sourceId, 0.0);
        pq.add(new NodeDistance(sourceId, 0.0));

        Set<String> visited = new HashSet<>();

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String currentId = current.nodeId;

            if (currentId.equals(targetId)) {
                break; // Found the shortest path to target
            }

            if (!visited.add(currentId)) {
                continue; // Already processed
            }

            for (Edge edge : graph.getEdges(currentId)) {
                String neighborId = edge.getTarget().getId();
                if (visited.contains(neighborId)) {
                    continue;
                }

                double newDist = distances.get(currentId) + edge.getWeight();
                if (newDist < distances.get(neighborId)) {
                    distances.put(neighborId, newDist);
                    previous.put(neighborId, currentId);
                    pq.add(new NodeDistance(neighborId, newDist));
                }
            }
        }

        if (distances.get(targetId) == Double.POSITIVE_INFINITY) {
            return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY);
        }

        List<String> path = new ArrayList<>();
        String curr = targetId;
        while (curr != null) {
            path.add(curr);
            curr = previous.get(curr);
        }
        Collections.reverse(path);

        return new PathResult(path, distances.get(targetId));
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
