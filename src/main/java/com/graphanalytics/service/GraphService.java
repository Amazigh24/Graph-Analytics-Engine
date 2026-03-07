package com.graphanalytics.service;

import com.graphanalytics.algorithm.*;
import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Node;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GraphService {

    private final Map<String, AdjacencyListGraph> graphs = new ConcurrentHashMap<>();

    public String createGraph(boolean isDirected) {
        String id = UUID.randomUUID().toString();
        graphs.put(id, new AdjacencyListGraph(isDirected));
        return id;
    }

    public void addEdges(String graphId, Iterable<EdgeDto> edges) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);

        for (EdgeDto edge : edges) {
            // Ensure nodes exist before adding edges
            if (graph.getNode(edge.getSource()).isEmpty()) {
                graph.addNode(new Node(edge.getSource()));
            }
            if (graph.getNode(edge.getTarget()).isEmpty()) {
                graph.addNode(new Node(edge.getTarget()));
            }

            if (graph.isDirected()) {
                graph.addEdge(edge.getSource(), edge.getTarget(), edge.getWeight());
            } else {
                graph.addUndirectedEdge(edge.getSource(), edge.getTarget(), edge.getWeight());
            }
        }
    }

    public AdjacencyListGraph getGraph(String id) {
        return graphs.get(id);
    }

    private AdjacencyListGraph getGraphOrThrow(String id) {
        AdjacencyListGraph graph = getGraph(id);
        if (graph == null) {
            throw new IllegalArgumentException("Graph not found: " + id);
        }
        return graph;
    }

    public Map<String, Double> runPageRank(String graphId, Double dampingFactor, Integer maxIterations) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        PageRankAlgorithm algo = new PageRankAlgorithm(
                dampingFactor != null ? dampingFactor : 0.85,
                maxIterations != null ? maxIterations : 100,
                1e-6);
        return algo.execute(graph);
    }

    public PathResult runDijkstra(String graphId, String sourceId, String targetId) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        DijkstraAlgorithm algo = new DijkstraAlgorithm(sourceId, targetId);
        return algo.execute(graph);
    }

    public PathResult runAStar(String graphId, String sourceId, String targetId) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        AStarAlgorithm algo = new AStarAlgorithm(sourceId, targetId);
        return algo.execute(graph);
    }

    public Map<String, String> runLouvain(String graphId) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        LouvainAlgorithm algo = new LouvainAlgorithm();
        return algo.execute(graph);
    }

    public static class EdgeDto {
        private String source;
        private String target;
        private double weight = 1.0;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }
    }
}
