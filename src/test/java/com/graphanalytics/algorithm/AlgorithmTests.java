package com.graphanalytics.algorithm;

import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Node;
import org.junit.jupiter.api.Test;

import java.util.List;
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
    void testPageRankEmptyGraph() {
        AdjacencyListGraph graph = new AdjacencyListGraph(true);
        PageRankAlgorithm pr = new PageRankAlgorithm();
        Map<String, Double> ranks = pr.execute(graph);
        assertTrue(ranks.isEmpty());
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
        assertIterableEquals(List.of("A", "B", "C", "D"), result.getPathNodeIds());
    }

    @Test
    void testDijkstraUnreachable() {
        AdjacencyListGraph graph = new AdjacencyListGraph(true);
        graph.addNode(new Node("A"));
        graph.addNode(new Node("B"));
        // No edges — B is unreachable from A

        DijkstraAlgorithm dijkstra = new DijkstraAlgorithm("A", "B");
        PathResult result = dijkstra.execute(graph);

        assertTrue(result.getPathNodeIds().isEmpty());
    }

    @Test
    void testAStar() {
        AdjacencyListGraph graph = new AdjacencyListGraph(false);
        Node a = new Node("A"); a.addProperty("x", 0.0); a.addProperty("y", 0.0);
        Node b = new Node("B"); b.addProperty("x", 1.0); b.addProperty("y", 0.0);
        Node c = new Node("C"); c.addProperty("x", 2.0); c.addProperty("y", 0.0);

        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(c);

        graph.addUndirectedEdge("A", "B", 1.0);
        graph.addUndirectedEdge("B", "C", 1.0);
        graph.addUndirectedEdge("A", "C", 3.0);

        AStarAlgorithm astar = new AStarAlgorithm("A", "C", new EuclideanHeuristic());
        PathResult result = astar.execute(graph);

        assertEquals(2.0, result.getTotalCost(), 0.001);
        assertIterableEquals(List.of("A", "B", "C"), result.getPathNodeIds());
    }

    @Test
    void testBfs() {
        AdjacencyListGraph graph = new AdjacencyListGraph(false);
        graph.addNode(new Node("A"));
        graph.addNode(new Node("B"));
        graph.addNode(new Node("C"));
        graph.addNode(new Node("D"));

        graph.addUndirectedEdge("A", "B", 1.0);
        graph.addUndirectedEdge("A", "C", 1.0);
        graph.addUndirectedEdge("B", "D", 1.0);

        BfsAlgorithm bfs = new BfsAlgorithm("A");
        List<String> result = bfs.execute(graph);

        assertEquals("A", result.get(0));
        assertEquals(4, result.size());
        // B and C should come before D
        assertTrue(result.indexOf("B") < result.indexOf("D"));
        assertTrue(result.indexOf("C") < result.indexOf("D"));
    }

    @Test
    void testDfs() {
        AdjacencyListGraph graph = new AdjacencyListGraph(false);
        graph.addNode(new Node("A"));
        graph.addNode(new Node("B"));
        graph.addNode(new Node("C"));

        graph.addUndirectedEdge("A", "B", 1.0);
        graph.addUndirectedEdge("B", "C", 1.0);

        DfsAlgorithm dfs = new DfsAlgorithm("A");
        List<String> result = dfs.execute(graph);

        assertEquals("A", result.get(0));
        assertEquals(3, result.size());
        assertTrue(result.contains("B"));
        assertTrue(result.contains("C"));
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

    @Test
    void testClosenessCentrality() {
        AdjacencyListGraph graph = new AdjacencyListGraph(false);
        graph.addNode(new Node("A"));
        graph.addNode(new Node("B"));
        graph.addNode(new Node("C"));

        // A--B--C (linear chain)
        graph.addUndirectedEdge("A", "B", 1.0);
        graph.addUndirectedEdge("B", "C", 1.0);

        ClosenessCentralityAlgorithm algo = new ClosenessCentralityAlgorithm();
        Map<String, Double> result = algo.execute(graph);

        // B is the most central node (distance 1 to both A and C)
        assertTrue(result.get("B") > result.get("A"));
        assertTrue(result.get("B") > result.get("C"));
    }

    @Test
    void testBetweennessCentralityUnweighted() {
        AdjacencyListGraph graph = new AdjacencyListGraph(false);
        graph.addNode(new Node("A"));
        graph.addNode(new Node("B"));
        graph.addNode(new Node("C"));

        // A--B--C (linear chain)
        graph.addUndirectedEdge("A", "B", 1.0);
        graph.addUndirectedEdge("B", "C", 1.0);

        BetweennessCentralityAlgorithm algo = new BetweennessCentralityAlgorithm(false);
        Map<String, Double> result = algo.execute(graph);

        // B should have the highest betweenness (all A-C paths go through B)
        assertTrue(result.get("B") > result.get("A"));
        assertTrue(result.get("B") > result.get("C"));
        assertEquals(0.0, result.get("A"), 0.001);
        assertEquals(0.0, result.get("C"), 0.001);
    }

    @Test
    void testBetweennessCentralityWeighted() {
        AdjacencyListGraph graph = new AdjacencyListGraph(false);
        graph.addNode(new Node("A"));
        graph.addNode(new Node("B"));
        graph.addNode(new Node("C"));

        graph.addUndirectedEdge("A", "B", 1.0);
        graph.addUndirectedEdge("B", "C", 1.0);
        graph.addUndirectedEdge("A", "C", 5.0);

        BetweennessCentralityAlgorithm algo = new BetweennessCentralityAlgorithm(true);
        Map<String, Double> result = algo.execute(graph);

        // B is on the shortest A-C path (cost 2 vs direct 5)
        assertTrue(result.get("B") > 0.0);
    }

    @Test
    void testGraphRemoveNode() {
        AdjacencyListGraph graph = new AdjacencyListGraph(false);
        graph.addNode(new Node("A"));
        graph.addNode(new Node("B"));
        graph.addNode(new Node("C"));
        graph.addUndirectedEdge("A", "B", 1.0);
        graph.addUndirectedEdge("B", "C", 1.0);

        assertTrue(graph.removeNode("B"));
        assertEquals(2, graph.getNodeCount());
        assertTrue(graph.getEdges("A").isEmpty());
        assertTrue(graph.getEdges("C").isEmpty());
        assertFalse(graph.removeNode("B")); // Already removed
    }

    @Test
    void testGraphRemoveEdge() {
        AdjacencyListGraph graph = new AdjacencyListGraph(true);
        graph.addNode(new Node("A"));
        graph.addNode(new Node("B"));
        graph.addEdge("A", "B", 1.0);

        assertTrue(graph.removeEdge("A", "B"));
        assertTrue(graph.getEdges("A").isEmpty());
        assertFalse(graph.removeEdge("A", "B")); // Already removed
    }
}
