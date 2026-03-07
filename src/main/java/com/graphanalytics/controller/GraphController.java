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
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @PostMapping
    public ResponseEntity<GraphCreatedResponse> createGraph(@RequestParam(defaultValue = "false") boolean directed) {
        String id = graphService.createGraph(directed);
        return ResponseEntity.ok(new GraphCreatedResponse(id));
    }

    @PostMapping("/{id}/edges")
    public ResponseEntity<Void> addEdges(@PathVariable String id, @RequestBody List<GraphService.EdgeDto> edges) {
        graphService.addEdges(id, edges);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/algorithms/pagerank")
    public ResponseEntity<Map<String, Double>> runPageRank(
            @PathVariable String id,
            @RequestParam(required = false) Double dampingFactor,
            @RequestParam(required = false) Integer maxIterations) {
        return ResponseEntity.ok(graphService.runPageRank(id, dampingFactor, maxIterations));
    }

    @PostMapping("/{id}/algorithms/shortest-path/dijkstra")
    public ResponseEntity<PathResult> runDijkstra(
            @PathVariable String id,
            @RequestParam String source,
            @RequestParam String target) {
        return ResponseEntity.ok(graphService.runDijkstra(id, source, target));
    }

    @PostMapping("/{id}/algorithms/shortest-path/astar")
    public ResponseEntity<PathResult> runAStar(
            @PathVariable String id,
            @RequestParam String source,
            @RequestParam String target) {
        return ResponseEntity.ok(graphService.runAStar(id, source, target));
    }

    @PostMapping("/{id}/algorithms/community/louvain")
    public ResponseEntity<Map<String, String>> runLouvain(@PathVariable String id) {
        return ResponseEntity.ok(graphService.runLouvain(id));
    }

    @Data
    public static class GraphCreatedResponse {
        private final String id;
    }
}
