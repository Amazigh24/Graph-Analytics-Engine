package com.graphanalytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphanalytics.domain.AdjacencyListGraph;
import com.graphanalytics.domain.Edge;
import com.graphanalytics.domain.Node;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GraphStorageService {

    private final ObjectMapper objectMapper;
    private final GraphService graphService;

    public GraphStorageService(ObjectMapper objectMapper, GraphService graphService) {
        this.objectMapper = objectMapper;
        this.graphService = graphService;
    }

    public void saveGraphToFile(String graphId, String filename) throws IOException {
        AdjacencyListGraph graph = graphService.getGraph(graphId);
        if (graph == null) {
            throw new IllegalArgumentException("Graph not found: " + graphId);
        }

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

        objectMapper.writeValue(new File(filename), dto);
    }

    public String loadGraphFromFile(String filename) throws IOException {
        GraphExportDto dto = objectMapper.readValue(new File(filename), GraphExportDto.class);

        String newGraphId = graphService.createGraph(dto.isDirected());
        AdjacencyListGraph newGraph = graphService.getGraph(newGraphId);

        if (dto.getNodes() != null) {
            for (NodeDto nDto : dto.getNodes()) {
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
