package com.graphanalytics.controller;

import com.graphanalytics.algorithm.PathResult;
import com.graphanalytics.service.GraphService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graphs")
@CrossOrigin(origins = "http://localhost:5173") // Default Vite dev port
public class GraphController {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    private final GraphService graphService;
    private final com.graphanalytics.service.GraphStorageService graphStorageService;

    public GraphController(GraphService graphService,
            com.graphanalytics.service.GraphStorageService graphStorageService) {
        this.graphService = graphService;
        this.graphStorageService = graphStorageService;
    }

    @PostMapping
    public ResponseEntity<GraphCreatedResponse> createGraph(@RequestParam(defaultValue = "false") boolean directed) {
        String id = graphService.createGraph(directed);
        return ResponseEntity.ok(new GraphCreatedResponse(id));
    }

    @PostMapping("/seed/random")
    public ResponseEntity<GraphCreatedResponse> seedRandomGraph(
            @RequestParam(defaultValue = "false") boolean directed,
            @RequestParam(defaultValue = "100") int nodes,
            @RequestParam(defaultValue = "300") int edges) {
        String id = graphService.seedRandomGraph(directed, nodes, edges);
        return ResponseEntity.ok(new GraphCreatedResponse(id));
    }

    @PostMapping("/{id}/edges")
    public ResponseEntity<Void> addEdges(@PathVariable String id, @RequestBody List<GraphService.EdgeDto> edges) {
        graphService.addEdges(id, edges);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/nodes")
    public ResponseEntity<List<com.graphanalytics.domain.Node>> getNodes(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(graphService.getNodesPaginated(id, page, size));
    }

    @GetMapping("/{id}/edges")
    public ResponseEntity<List<com.graphanalytics.domain.Edge>> getEdges(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(graphService.getEdgesPaginated(id, page, size));
    }

    @PostMapping("/{id}/algorithms/pagerank")
    public ResponseEntity<com.graphanalytics.algorithm.AlgorithmResultWrapper<Map<String, Double>>> runPageRank(
            @PathVariable String id,
            @RequestParam(required = false) Double dampingFactor,
            @RequestParam(required = false) Integer maxIterations) {
        long start = System.currentTimeMillis();
        Map<String, Double> result = graphService.runPageRank(id, dampingFactor, maxIterations);
        return ResponseEntity.ok(
                new com.graphanalytics.algorithm.AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @PostMapping("/{id}/algorithms/shortest-path/dijkstra")
    public ResponseEntity<com.graphanalytics.algorithm.AlgorithmResultWrapper<PathResult>> runDijkstra(
            @PathVariable String id,
            @RequestParam String source,
            @RequestParam String target) {
        long start = System.currentTimeMillis();
        PathResult result = graphService.runDijkstra(id, source, target);
        return ResponseEntity.ok(
                new com.graphanalytics.algorithm.AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @PostMapping("/{id}/algorithms/shortest-path/astar")
    public ResponseEntity<com.graphanalytics.algorithm.AlgorithmResultWrapper<PathResult>> runAStar(
            @PathVariable String id,
            @RequestParam String source,
            @RequestParam String target) {
        long start = System.currentTimeMillis();
        PathResult result = graphService.runAStar(id, source, target);
        return ResponseEntity.ok(
                new com.graphanalytics.algorithm.AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @PostMapping("/{id}/algorithms/community/louvain")
    public ResponseEntity<com.graphanalytics.algorithm.AlgorithmResultWrapper<Map<String, String>>> runLouvain(
            @PathVariable String id) {
        long start = System.currentTimeMillis();
        Map<String, String> result = graphService.runLouvain(id);
        return ResponseEntity.ok(
                new com.graphanalytics.algorithm.AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @PostMapping("/{id}/algorithms/traversal/bfs")
    public ResponseEntity<com.graphanalytics.algorithm.AlgorithmResultWrapper<List<String>>> runBfs(
            @PathVariable String id, @RequestParam String source) {
        long start = System.currentTimeMillis();
        List<String> result = graphService.runBfs(id, source);
        return ResponseEntity.ok(
                new com.graphanalytics.algorithm.AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @PostMapping("/{id}/algorithms/traversal/dfs")
    public ResponseEntity<com.graphanalytics.algorithm.AlgorithmResultWrapper<List<String>>> runDfs(
            @PathVariable String id, @RequestParam String source) {
        long start = System.currentTimeMillis();
        List<String> result = graphService.runDfs(id, source);
        return ResponseEntity.ok(
                new com.graphanalytics.algorithm.AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @PostMapping("/{id}/algorithms/centrality/closeness")
    public ResponseEntity<com.graphanalytics.algorithm.AlgorithmResultWrapper<Map<String, Double>>> runClosenessCentrality(
            @PathVariable String id) {
        long start = System.currentTimeMillis();
        Map<String, Double> result = graphService.runClosenessCentrality(id);
        return ResponseEntity.ok(
                new com.graphanalytics.algorithm.AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @PostMapping("/{id}/algorithms/centrality/betweenness")
    public ResponseEntity<com.graphanalytics.algorithm.AlgorithmResultWrapper<Map<String, Double>>> runBetweennessCentrality(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean weighted) {
        long start = System.currentTimeMillis();
        Map<String, Double> result = graphService.runBetweennessCentrality(id, weighted);
        return ResponseEntity.ok(
                new com.graphanalytics.algorithm.AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<Void> saveGraph(@PathVariable String id, @RequestParam String filename) {
        try {
            graphStorageService.saveGraphToFile(id, filename);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/load")
    public ResponseEntity<GraphCreatedResponse> loadGraph(@RequestParam String filename) {
        try {
            String newId = graphStorageService.loadGraphFromFile(filename);
            return ResponseEntity.ok(new GraphCreatedResponse(newId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Data
    public static class GraphCreatedResponse {
        private final String id;
    }
}
