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

        m = m / 2.0; // Since each undirected edge is counted twice
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

                String bestComm = currentComm;
                double maxModularityGain = 0.0;

                // Calculate weight of edges from node to other communities
                Map<String, Double> commWeights = new HashMap<>();
                for (Edge e : graph.getEdges(nodeId)) {
                    String neighborComm = communities.get(e.getTarget().getId());
                    if (neighborComm.equals(currentComm))
                        continue;
                    commWeights.put(neighborComm, commWeights.getOrDefault(neighborComm, 0.0) + e.getWeight());
                }

                // Find best community to join
                for (Map.Entry<String, Double> entry : commWeights.entrySet()) {
                    String candidateComm = entry.getKey();
                    double k_i_in = entry.getValue();
                    double tot_candidate = tot.get(candidateComm);

                    // Modularity gain formula
                    double gain = (k_i_in - resolution * (tot_candidate * nodeWeight) / (2 * m));

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
