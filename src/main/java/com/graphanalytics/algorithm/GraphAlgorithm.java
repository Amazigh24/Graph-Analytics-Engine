package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;

/**
 * Strategy interface for running graph algorithms.
 * 
 * @param <T> The result type of the algorithm.
 */
public interface GraphAlgorithm<T> {

    /**
     * Executes the algorithm on the given graph.
     * 
     * @param graph The graph to process.
     * @return The result of the algorithm.
     */
    T execute(AdjacencyListGraph graph);

}
