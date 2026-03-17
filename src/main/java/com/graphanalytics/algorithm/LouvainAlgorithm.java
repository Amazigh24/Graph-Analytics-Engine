package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Edge;
import com.graphanalytics.domain.Node;

import java.util.*;

/**
 * A simplified single-pass implementation of the Louvain algorithm for
 * community detection.
 * Returns a mapping of Node ID to its Community ID.
 */
public class LouvainAlgorithm implements GraphAlgorithm<Map<String, String>> {

    private final double resolution;
    private final int maxIterations;

    public LouvainAlgorithm(double resolution, int maxIterations) {
        this.resolution = resolution;
        this.maxIterations = maxIterations;
    }

    public LouvainAlgorithm() {
        this(1.0, 100);
    }

    @Override
    public Map<String, String> execute(AdjacencyListGraph graph) {
        Map<String, String> communities = new HashMap<>();
        Map<String, Double> k_i = new HashMap<>();
        Map<String, Double> tot = new HashMap<>(); // Sum of weights of edges incident to nodes in community c

        double m = 0.0; // Total weight of all edges in the graph

        // Initialize: each node is in its own community
        for (Node node : graph.getNodes()) {
            String nodeId = node.getId();
            communities.put(nodeId, nodeId);

            double nodeDegree = 0.0;
            for (Edge e : graph.getEdges(nodeId)) {
                nodeDegree += e.getWeight();
            }
            k_i.put(nodeId, nodeDegree);
            tot.put(nodeId, nodeDegree);
            m += nodeDegree;
        }

        // For undirected graphs, each edge is counted twice in the sum above
        if (!graph.isDirected()) {
            m = m / 2.0;
        }
        if (m == 0) {
            return communities;
        }

        boolean improvement = true;
        int iter = 0;

        while (improvement && iter < maxIterations) {
            improvement = false;
            iter++;

            for (Node node : graph.getNodes()) {
                String nodeId = node.getId();
                String currentComm = communities.get(nodeId);
                double nodeWeight = k_i.get(nodeId);

                // Remove node from its current community
                tot.put(currentComm, tot.get(currentComm) - nodeWeight);

                // Calculate weight of edges from node to each neighboring community
                // (including the node's own current community for comparison)
                Map<String, Double> commWeights = new HashMap<>();
                for (Edge e : graph.getEdges(nodeId)) {
                    String neighborComm = communities.get(e.getTarget().getId());
                    commWeights.put(neighborComm, commWeights.getOrDefault(neighborComm, 0.0) + e.getWeight());
                }

                String bestComm = currentComm;
                double maxModularityGain = 0.0;

                // Also evaluate staying in currentComm vs moving
                double k_i_in_current = commWeights.getOrDefault(currentComm, 0.0);
                double tot_current = tot.getOrDefault(currentComm, 0.0);
                double gainStay = k_i_in_current - resolution * (tot_current * nodeWeight) / (2 * m);

                // Find best community to join
                for (Map.Entry<String, Double> entry : commWeights.entrySet()) {
                    String candidateComm = entry.getKey();
                    if (candidateComm.equals(currentComm)) continue;

                    double k_i_in = entry.getValue();
                    double tot_candidate = tot.getOrDefault(candidateComm, 0.0);

                    // Modularity gain relative to staying
                    double gain = (k_i_in - resolution * (tot_candidate * nodeWeight) / (2 * m)) - gainStay;

                    if (gain > maxModularityGain) {
                        maxModularityGain = gain;
                        bestComm = candidateComm;
                    }
                }

                // Add node to the chosen community
                if (!bestComm.equals(currentComm)) {
                    communities.put(nodeId, bestComm);
                    tot.put(bestComm, tot.getOrDefault(bestComm, 0.0) + nodeWeight);
                    improvement = true;
                } else {
                    tot.put(currentComm, tot.get(currentComm) + nodeWeight); // Revert removal
                }
            }
        }

        return communities;
    }
}
