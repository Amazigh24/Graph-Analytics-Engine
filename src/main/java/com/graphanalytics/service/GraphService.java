package com.graphanalytics.service;

import com.graphanalytics.algorithm.*;
import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Node;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public String seedRandomGraph(boolean isDirected, int numNodes, int numEdges) {
        String id = createGraph(isDirected);
        AdjacencyListGraph graph = getGraphOrThrow(id);

        List<String> nodeIds = new java.util.ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            String nodeId = "N" + i;
            graph.addNode(new Node(nodeId));
            nodeIds.add(nodeId);
        }

        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < numEdges; i++) {
            String source = nodeIds.get(rand.nextInt(numNodes));
            String target = nodeIds.get(rand.nextInt(numNodes));
            // Only add edge if it's not a self-loop
            if (!source.equals(target)) {
                double weight = Math.round(rand.nextDouble() * 10.0 * 100.0) / 100.0;
                if (isDirected) {
                    graph.addEdge(source, target, weight);
                } else {
                    graph.addUndirectedEdge(source, target, weight);
                }
            }
        }
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
        GraphValidator.validateNotEmpty(graph);
        PageRankAlgorithm algo = new PageRankAlgorithm(
                dampingFactor != null ? dampingFactor : 0.85,
                maxIterations != null ? maxIterations : 100,
                1e-6);
        return algo.execute(graph);
    }

    public PathResult runDijkstra(String graphId, String sourceId, String targetId) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        GraphValidator.validateNotEmpty(graph);
        GraphValidator.validateNoNegativeWeights(graph);
        DijkstraAlgorithm algo = new DijkstraAlgorithm(sourceId, targetId);
        return algo.execute(graph);
    }

    public PathResult runAStar(String graphId, String sourceId, String targetId) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        GraphValidator.validateNotEmpty(graph);
        GraphValidator.validateNoNegativeWeights(graph);
        AStarAlgorithm algo = new AStarAlgorithm(sourceId, targetId, new EuclideanHeuristic());
        return algo.execute(graph);
    }

    public Map<String, String> runLouvain(String graphId) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        GraphValidator.validateNotEmpty(graph);
        LouvainAlgorithm algo = new LouvainAlgorithm();
        return algo.execute(graph);
    }

    public java.util.List<String> runBfs(String graphId, String sourceId) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        GraphValidator.validateNotEmpty(graph);
        BfsAlgorithm algo = new BfsAlgorithm(sourceId);
        return algo.execute(graph);
    }

    public java.util.List<String> runDfs(String graphId, String sourceId) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        GraphValidator.validateNotEmpty(graph);
        DfsAlgorithm algo = new DfsAlgorithm(sourceId);
        return algo.execute(graph);
    }

    public Map<String, Double> runClosenessCentrality(String graphId) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        GraphValidator.validateNotEmpty(graph);
        ClosenessCentralityAlgorithm algo = new ClosenessCentralityAlgorithm();
        return algo.execute(graph);
    }

    public Map<String, Double> runBetweennessCentrality(String graphId, boolean isWeighted) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        GraphValidator.validateNotEmpty(graph);
        if (isWeighted) {
            GraphValidator.validateNoNegativeWeights(graph);
        }
        BetweennessCentralityAlgorithm algo = new BetweennessCentralityAlgorithm(isWeighted);
        return algo.execute(graph);
    }

    public List<Node> getNodesPaginated(String graphId, int page, int size) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        return graph.getNodes().stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    public List<com.graphanalytics.domain.Edge> getEdgesPaginated(String graphId, int page, int size) {
        AdjacencyListGraph graph = getGraphOrThrow(graphId);
        return graph.getNodes().stream()
                .flatMap(node -> graph.getEdges(node.getId()).stream())
                .skip((long) page * size)
                .limit(size)
                .toList();
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
