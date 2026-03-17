package com.graphanalytics.controller;

import com.graphanalytics.algorithm.AlgorithmResultWrapper;
import com.graphanalytics.algorithm.PathResult;
import com.graphanalytics.domain.Edge;
import com.graphanalytics.domain.Node;
import com.graphanalytics.service.GraphService;
import com.graphanalytics.service.GraphStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graphs")
public class GraphController {

    private final GraphService graphService;
    private final GraphStorageService graphStorageService;

    public GraphController(GraphService graphService, GraphStorageService graphStorageService) {
        this.graphService = graphService;
        this.graphStorageService = graphStorageService;
    }

    @Operation(summary = "Create an empty graph")
    @ApiResponse(responseCode = "200", description = "Graph created successfully")
    @Tag(name = "Graph Management")
    @PostMapping
    public ResponseEntity<GraphCreatedResponse> createGraph(@RequestParam(defaultValue = "false") boolean directed) {
        String id = graphService.createGraph(directed);
        return ResponseEntity.ok(new GraphCreatedResponse(id));
    }

    @Operation(summary = "Create and seed a graph with random nodes and edges")
    @ApiResponse(responseCode = "200", description = "Random graph created successfully")
    @Tag(name = "Graph Management")
    @PostMapping("/seed/random")
    public ResponseEntity<GraphCreatedResponse> seedRandomGraph(
            @RequestParam(defaultValue = "false") boolean directed,
            @Parameter(description = "Number of nodes (1-10000)") @RequestParam(defaultValue = "100") int nodes,
            @Parameter(description = "Number of edges (0-100000)") @RequestParam(defaultValue = "300") int edges) {
        if (nodes < 1 || nodes > 10000) {
            throw new IllegalArgumentException("Node count must be between 1 and 10000.");
        }
        if (edges < 0 || edges > 100000) {
            throw new IllegalArgumentException("Edge count must be between 0 and 100000.");
        }
        String id = graphService.seedRandomGraph(directed, nodes, edges);
        return ResponseEntity.ok(new GraphCreatedResponse(id));
    }

    @Operation(summary = "Add edges to an existing graph")
    @ApiResponse(responseCode = "200", description = "Edges added successfully")
    @Tag(name = "Graph Management")
    @PostMapping("/{id}/edges")
    public ResponseEntity<Void> addEdges(@PathVariable String id, @RequestBody List<GraphService.EdgeDto> edges) {
        graphService.addEdges(id, edges);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get paginated nodes of a graph")
    @Tag(name = "Graph Management")
    @GetMapping("/{id}/nodes")
    public ResponseEntity<List<Node>> getNodes(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        if (page < 0) throw new IllegalArgumentException("Page must be >= 0.");
        if (size < 1 || size > 1000) throw new IllegalArgumentException("Size must be between 1 and 1000.");
        return ResponseEntity.ok(graphService.getNodesPaginated(id, page, size));
    }

    @Operation(summary = "Get paginated edges of a graph")
    @Tag(name = "Graph Management")
    @GetMapping("/{id}/edges")
    public ResponseEntity<List<Edge>> getEdges(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        if (page < 0) throw new IllegalArgumentException("Page must be >= 0.");
        if (size < 1 || size > 1000) throw new IllegalArgumentException("Size must be between 1 and 1000.");
        return ResponseEntity.ok(graphService.getEdgesPaginated(id, page, size));
    }

    @Operation(summary = "Run PageRank algorithm on a graph")
    @Tag(name = "Algorithms")
    @PostMapping("/{id}/algorithms/pagerank")
    public ResponseEntity<AlgorithmResultWrapper<Map<String, Double>>> runPageRank(
            @PathVariable String id,
            @Parameter(description = "Damping factor (0.0-1.0)") @RequestParam(required = false) Double dampingFactor,
            @Parameter(description = "Max iterations (1-10000)") @RequestParam(required = false) Integer maxIterations) {
        if (dampingFactor != null && (dampingFactor < 0.0 || dampingFactor > 1.0)) {
            throw new IllegalArgumentException("Damping factor must be between 0.0 and 1.0.");
        }
        if (maxIterations != null && (maxIterations < 1 || maxIterations > 10000)) {
            throw new IllegalArgumentException("Max iterations must be between 1 and 10000.");
        }
        long start = System.currentTimeMillis();
        Map<String, Double> result = graphService.runPageRank(id, dampingFactor, maxIterations);
        return ResponseEntity.ok(new AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @Operation(summary = "Find shortest path using Dijkstra's algorithm")
    @Tag(name = "Algorithms")
    @PostMapping("/{id}/algorithms/shortest-path/dijkstra")
    public ResponseEntity<AlgorithmResultWrapper<PathResult>> runDijkstra(
            @PathVariable String id,
            @RequestParam String source,
            @RequestParam String target) {
        long start = System.currentTimeMillis();
        PathResult result = graphService.runDijkstra(id, source, target);
        return ResponseEntity.ok(new AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @Operation(summary = "Find shortest path using A* algorithm")
    @Tag(name = "Algorithms")
    @PostMapping("/{id}/algorithms/shortest-path/astar")
    public ResponseEntity<AlgorithmResultWrapper<PathResult>> runAStar(
            @PathVariable String id,
            @RequestParam String source,
            @RequestParam String target) {
        long start = System.currentTimeMillis();
        PathResult result = graphService.runAStar(id, source, target);
        return ResponseEntity.ok(new AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @Operation(summary = "Detect communities using Louvain algorithm")
    @Tag(name = "Algorithms")
    @PostMapping("/{id}/algorithms/community/louvain")
    public ResponseEntity<AlgorithmResultWrapper<Map<String, String>>> runLouvain(@PathVariable String id) {
        long start = System.currentTimeMillis();
        Map<String, String> result = graphService.runLouvain(id);
        return ResponseEntity.ok(new AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @Operation(summary = "Run breadth-first search traversal")
    @Tag(name = "Algorithms")
    @PostMapping("/{id}/algorithms/traversal/bfs")
    public ResponseEntity<AlgorithmResultWrapper<List<String>>> runBfs(
            @PathVariable String id, @RequestParam String source) {
        long start = System.currentTimeMillis();
        List<String> result = graphService.runBfs(id, source);
        return ResponseEntity.ok(new AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @Operation(summary = "Run depth-first search traversal")
    @Tag(name = "Algorithms")
    @PostMapping("/{id}/algorithms/traversal/dfs")
    public ResponseEntity<AlgorithmResultWrapper<List<String>>> runDfs(
            @PathVariable String id, @RequestParam String source) {
        long start = System.currentTimeMillis();
        List<String> result = graphService.runDfs(id, source);
        return ResponseEntity.ok(new AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @Operation(summary = "Compute closeness centrality for all nodes")
    @Tag(name = "Algorithms")
    @PostMapping("/{id}/algorithms/centrality/closeness")
    public ResponseEntity<AlgorithmResultWrapper<Map<String, Double>>> runClosenessCentrality(@PathVariable String id) {
        long start = System.currentTimeMillis();
        Map<String, Double> result = graphService.runClosenessCentrality(id);
        return ResponseEntity.ok(new AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @Operation(summary = "Compute betweenness centrality for all nodes")
    @Tag(name = "Algorithms")
    @PostMapping("/{id}/algorithms/centrality/betweenness")
    public ResponseEntity<AlgorithmResultWrapper<Map<String, Double>>> runBetweennessCentrality(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean weighted) {
        long start = System.currentTimeMillis();
        Map<String, Double> result = graphService.runBetweennessCentrality(id, weighted);
        return ResponseEntity.ok(new AlgorithmResultWrapper<>(result, System.currentTimeMillis() - start));
    }

    @Operation(summary = "Save a graph to a file")
    @ApiResponse(responseCode = "200", description = "Graph saved successfully")
    @ApiResponse(responseCode = "500", description = "I/O error during save")
    @Tag(name = "Persistence")
    @PostMapping("/{id}/save")
    public ResponseEntity<Void> saveGraph(@PathVariable String id, @RequestParam String filename) {
        try {
            graphStorageService.saveGraphToFile(id, filename);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Load a graph from a file")
    @ApiResponse(responseCode = "200", description = "Graph loaded successfully")
    @ApiResponse(responseCode = "500", description = "I/O error during load")
    @Tag(name = "Persistence")
    @PostMapping("/load")
    public ResponseEntity<GraphCreatedResponse> loadGraph(@RequestParam String filename) {
        try {
            String newId = graphStorageService.loadGraphFromFile(filename);
            return ResponseEntity.ok(new GraphCreatedResponse(newId));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Data
    public static class GraphCreatedResponse {
        private final String id;
    }
}
