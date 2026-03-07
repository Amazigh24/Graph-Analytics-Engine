package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Edge;
import com.graphanalytics.domain.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageRankAlgorithm implements GraphAlgorithm<Map<String, Double>> {

    private final double dampingFactor;
    private final int maxIterations;
    private final double tolerance;

    public PageRankAlgorithm(double dampingFactor, int maxIterations, double tolerance) {
        this.dampingFactor = dampingFactor;
        this.maxIterations = maxIterations;
        this.tolerance = tolerance;
    }

    public PageRankAlgorithm() {
        this(0.85, 100, 1e-6);
    }

    @Override
    public Map<String, Double> execute(AdjacencyListGraph graph) {
        long numNodes = graph.getNodeCount();
        if (numNodes == 0) {
            return new HashMap<>();
        }

        Map<String, Double> currentRanks = new HashMap<>();
        Map<String, Double> nextRanks = new HashMap<>();

        double initialRank = 1.0 / numNodes;
        for (Node node : graph.getNodes()) {
            currentRanks.put(node.getId(), initialRank);
        }

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            double diff = 0.0;
            double danglingSum = 0.0;

            // Calculate sum of ranks from dangling nodes (nodes with no out-edges)
            for (Node node : graph.getNodes()) {
                if (graph.getEdges(node.getId()).isEmpty()) {
                    danglingSum += currentRanks.get(node.getId());
                }
            }

            for (Node node : graph.getNodes()) {
                double rankSum = 0.0;
                List<Edge> incomingEdges = graph.getIncomingEdges(node.getId());

                for (Edge inEdge : incomingEdges) {
                    String sourceId = inEdge.getSource().getId();
                    int outDegree = graph.getEdges(sourceId).size();
                    if (outDegree > 0) {
                        rankSum += currentRanks.get(sourceId) / outDegree;
                    }
                }

                double newRank = ((1.0 - dampingFactor) / numNodes)
                        + (dampingFactor * (rankSum + (danglingSum / numNodes)));

                nextRanks.put(node.getId(), newRank);
                diff += Math.abs(newRank - currentRanks.get(node.getId()));
            }

            // Swap maps
            Map<String, Double> temp = currentRanks;
            currentRanks = nextRanks;
            nextRanks = temp;

            if (diff < tolerance) {
                break;
            }
        }

        return currentRanks;
    }
}
