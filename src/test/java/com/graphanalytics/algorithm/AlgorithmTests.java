package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Node;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlgorithmTests {

    @Test
    void testPageRank() {
        AdjacencyListGraph graph = new AdjacencyListGraph(true);
        graph.addNode(new Node("A"));
        graph.addNode(new Node("B"));
        graph.addNode(new Node("C"));

        graph.addEdge("A", "B", 1.0);
        graph.addEdge("B", "C", 1.0);
        graph.addEdge("C", "A", 1.0);

        PageRankAlgorithm pr = new PageRankAlgorithm(0.85, 100, 1e-6);
        Map<String, Double> ranks = pr.execute(graph);

        // In a simple triangle, they should have equal rank approx 0.333
        assertEquals(3, ranks.size());
        assertTrue(Math.abs(ranks.get("A") - 0.333) < 0.01);
        assertTrue(Math.abs(ranks.get("B") - 0.333) < 0.01);
        assertTrue(Math.abs(ranks.get("C") - 0.333) < 0.01);
    }

    @Test
    void testDijkstra() {
        AdjacencyListGraph graph = new AdjacencyListGraph(false);
        graph.addNode(new Node("A"));
        graph.addNode(new Node("B"));
        graph.addNode(new Node("C"));
        graph.addNode(new Node("D"));

        graph.addUndirectedEdge("A", "B", 1.0);
        graph.addUndirectedEdge("B", "C", 2.0);
        graph.addUndirectedEdge("A", "C", 4.0);
        graph.addUndirectedEdge("C", "D", 1.0);

        DijkstraAlgorithm dijkstra = new DijkstraAlgorithm("A", "D");
        PathResult result = dijkstra.execute(graph);

        assertEquals(4.0, result.getTotalCost(), 0.001);
        assertIterableEquals(java.util.List.of("A", "B", "C", "D"), result.getPathNodeIds());
    }

    @Test
    void testLouvain() {
        AdjacencyListGraph graph = new AdjacencyListGraph(false);
        graph.addNode(new Node("A"));
        graph.addNode(new Node("B"));
        graph.addNode(new Node("C"));
        graph.addNode(new Node("D"));

        // Clique 1
        graph.addUndirectedEdge("A", "B", 1.0);

        // Clique 2
        graph.addUndirectedEdge("C", "D", 1.0);

        // Weak bridge
        graph.addUndirectedEdge("B", "C", 0.1);

        LouvainAlgorithm louvain = new LouvainAlgorithm(1.0, 10);
        Map<String, String> communities = louvain.execute(graph);

        assertEquals(communities.get("A"), communities.get("B"));
        assertEquals(communities.get("C"), communities.get("D"));
        assertNotEquals(communities.get("A"), communities.get("C"));
    }
}
