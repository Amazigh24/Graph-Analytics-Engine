package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Edge;
import com.graphanalytics.domain.Node;

import java.util.*;

/**
 * Implementation of Brandes' algorithm for calculating Betweenness Centrality.
 * It handles both unweighted (using BFS) and weighted (using Dijkstra) graphs.
 * Time complexity: O(VE) for unweighted, O(VE + V^2 log V) for weighted.
 */
public class BetweennessCentralityAlgorithm implements GraphAlgorithm<Map<String, Double>> {

    private final boolean isWeighted;

    public BetweennessCentralityAlgorithm(boolean isWeighted) {
        this.isWeighted = isWeighted;
    }

    @Override
    public Map<String, Double> execute(AdjacencyListGraph graph) {
        Map<String, Double> centrality = new HashMap<>();
        for (Node node : graph.getNodes()) {
            centrality.put(node.getId(), 0.0);
        }

        for (Node s : graph.getNodes()) {
            Stack<String> S = new Stack<>();
            Map<String, List<String>> P = new HashMap<>(); // Predecessors
            Map<String, Double> sigma = new HashMap<>(); // Number of shortest paths
            Map<String, Double> d = new HashMap<>(); // Distances

            for (Node v : graph.getNodes()) {
                P.put(v.getId(), new ArrayList<>());
                sigma.put(v.getId(), 0.0);
                d.put(v.getId(), Double.POSITIVE_INFINITY);
            }

            String sId = s.getId();
            sigma.put(sId, 1.0);
            d.put(sId, 0.0);

            if (isWeighted) {
                runDijkstra(graph, sId, S, P, sigma, d);
            } else {
                runBfs(graph, sId, S, P, sigma, d);
            }

            Map<String, Double> delta = new HashMap<>();
            for (Node v : graph.getNodes()) {
                delta.put(v.getId(), 0.0);
            }

            while (!S.isEmpty()) {
                String w = S.pop();
                for (String v : P.get(w)) {
                    double c = (sigma.get(v) / sigma.get(w)) * (1.0 + delta.get(w));
                    delta.put(v, delta.get(v) + c);
                }
                if (!w.equals(sId)) {
                    centrality.put(w, centrality.get(w) + delta.get(w));
                }
            }
        }

        // For undirected graphs, betweenness scores are doubled, so we halve them.
        if (!graph.isDirected()) {
            for (Map.Entry<String, Double> entry : centrality.entrySet()) {
                centrality.put(entry.getKey(), entry.getValue() / 2.0);
            }
        }

        return centrality;
    }

    private void runBfs(AdjacencyListGraph graph, String sId, Stack<String> S,
            Map<String, List<String>> P, Map<String, Double> sigma, Map<String, Double> d) {
        Queue<String> Q = new LinkedList<>();
        Q.add(sId);

        while (!Q.isEmpty()) {
            String v = Q.poll();
            S.push(v);

            for (Edge edge : graph.getEdges(v)) {
                String w = edge.getTarget().getId();
                // w found for the first time?
                if (d.get(w) == Double.POSITIVE_INFINITY) {
                    Q.add(w);
                    d.put(w, d.get(v) + 1.0);
                }
                // Shortest path to w via v?
                if (d.get(w) == d.get(v) + 1.0) {
                    sigma.put(w, sigma.get(w) + sigma.get(v));
                    P.get(w).add(v);
                }
            }
        }
    }

    private void runDijkstra(AdjacencyListGraph graph, String sId, Stack<String> S,
            Map<String, List<String>> P, Map<String, Double> sigma, Map<String, Double> d) {
        PriorityQueue<NodeDistance> Q = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));
        Q.add(new NodeDistance(sId, 0.0));
        Set<String> expanded = new HashSet<>();

        while (!Q.isEmpty()) {
            NodeDistance current = Q.poll();
            String v = current.nodeId;

            if (expanded.contains(v)) {
                continue;
            }
            expanded.add(v);
            S.push(v);

            for (Edge edge : graph.getEdges(v)) {
                String w = edge.getTarget().getId();
                double weight = edge.getWeight();
                double newDist = d.get(v) + weight;

                if (newDist < d.get(w)) {
                    d.put(w, newDist);
                    Q.add(new NodeDistance(w, newDist));
                    sigma.put(w, sigma.get(v));
                    P.get(w).clear();
                    P.get(w).add(v);
                } else if (newDist == d.get(w)) {
                    sigma.put(w, sigma.get(w) + sigma.get(v));
                    P.get(w).add(v);
                }
            }
        }
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
