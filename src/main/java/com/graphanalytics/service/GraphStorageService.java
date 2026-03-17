package com.graphanalytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Edge;
import com.graphanalytics.domain.Node;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GraphStorageService {

    private static final Path STORAGE_DIR = Paths.get("graph-data").toAbsolutePath().normalize();

    private final ObjectMapper objectMapper;
    private final GraphService graphService;

    public GraphStorageService(ObjectMapper objectMapper, GraphService graphService) {
        this.objectMapper = objectMapper;
        this.graphService = graphService;
    }

    private Path resolveAndValidateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename must not be empty.");
        }
        // Strip any path separators — only allow a plain filename
        String sanitized = Paths.get(filename).getFileName().toString();
        if (!sanitized.matches("^[a-zA-Z0-9_\\-]+\\.json$")) {
            throw new IllegalArgumentException("Filename must be alphanumeric (with dashes/underscores) and end with .json");
        }
        try {
            Path resolved = STORAGE_DIR.resolve(sanitized).normalize();
            if (!resolved.startsWith(STORAGE_DIR)) {
                throw new IllegalArgumentException("Invalid filename — path traversal detected.");
            }
            return resolved;
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid filename.");
        }
    }

    public void saveGraphToFile(String graphId, String filename) throws IOException {
        AdjacencyListGraph graph = graphService.getGraph(graphId);
        if (graph == null) {
            throw new IllegalArgumentException("Graph not found: " + graphId);
        }

        Path filePath = resolveAndValidateFilename(filename);
        Files.createDirectories(STORAGE_DIR);

        GraphExportDto dto = new GraphExportDto();
        dto.setDirected(graph.isDirected());

        List<NodeDto> nodes = new ArrayList<>();
        for (Node node : graph.getNodes()) {
            NodeDto nDto = new NodeDto();
            nDto.setId(node.getId());
            nDto.setProperties(node.getProperties());
            nodes.add(nDto);
        }
        dto.setNodes(nodes);

        List<GraphService.EdgeDto> edges = new ArrayList<>();
        for (Node node : graph.getNodes()) {
            for (Edge edge : graph.getEdges(node.getId())) {
                GraphService.EdgeDto eDto = new GraphService.EdgeDto();
                eDto.setSource(edge.getSource().getId());
                eDto.setTarget(edge.getTarget().getId());
                eDto.setWeight(edge.getWeight());
                edges.add(eDto);
            }
        }
        dto.setEdges(edges);

        objectMapper.writeValue(filePath.toFile(), dto);
    }

    public String loadGraphFromFile(String filename) throws IOException {
        Path filePath = resolveAndValidateFilename(filename);
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found: " + filename);
        }

        GraphExportDto dto = objectMapper.readValue(filePath.toFile(), GraphExportDto.class);

        String newGraphId = graphService.createGraph(dto.isDirected());
        AdjacencyListGraph newGraph = graphService.getGraph(newGraphId);
        if (newGraph == null) {
            throw new IllegalStateException("Failed to create graph.");
        }

        if (dto.getNodes() != null) {
            for (NodeDto nDto : dto.getNodes()) {
                if (nDto == null || nDto.getId() == null) continue;
                Node node = new Node(nDto.getId());
                if (nDto.getProperties() != null) {
                    node.getProperties().putAll(nDto.getProperties());
                }
                newGraph.addNode(node);
            }
        }

        if (dto.getEdges() != null) {
            graphService.addEdges(newGraphId, dto.getEdges());
        }

        return newGraphId;
    }

    public static class GraphExportDto {
        private boolean directed;
        private List<NodeDto> nodes;
        private List<GraphService.EdgeDto> edges;

        public boolean isDirected() {
            return directed;
        }

        public void setDirected(boolean directed) {
            this.directed = directed;
        }

        public List<NodeDto> getNodes() {
            return nodes;
        }

        public void setNodes(List<NodeDto> nodes) {
            this.nodes = nodes;
        }

        public List<GraphService.EdgeDto> getEdges() {
            return edges;
        }

        public void setEdges(List<GraphService.EdgeDto> edges) {
            this.edges = edges;
        }
    }

    public static class NodeDto {
        private String id;
        private Map<String, Object> properties;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }
}
