package com.graphanalytics.domain;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AdjacencyListGraph implements Graph {

    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private final Map<String, List<Edge>> adjacencyList = new ConcurrentHashMap<>();

    // For fast retrieval of incoming edges in certain algorithms
    private final Map<String, List<Edge>> incomingEdges = new ConcurrentHashMap<>();

    private boolean isDirected;

    public AdjacencyListGraph(boolean isDirected) {
        this.isDirected = isDirected;
    }

    @Override
    public void addNode(Node node) {
        nodes.putIfAbsent(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new CopyOnWriteArrayList<>());
        incomingEdges.putIfAbsent(node.getId(), new CopyOnWriteArrayList<>());
    }

    @Override
    public void addEdge(String sourceId, String targetId, double weight) {
        Node source = nodes.get(sourceId);
        Node target = nodes.get(targetId);

        if (source == null || target == null) {
            throw new IllegalArgumentException("Source or target node does not exist in the graph");
        }

        Edge edge = new Edge(source, target, weight);
        adjacencyList.get(sourceId).add(edge);
        incomingEdges.get(targetId).add(edge);
    }

    @Override
    public void addUndirectedEdge(String sourceId, String targetId, double weight) {
        addEdge(sourceId, targetId, weight);
        addEdge(targetId, sourceId, weight);
    }

    @Override
    public Optional<Node> getNode(String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    @Override
    public Collection<Node> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    @Override
    public List<Edge> getEdges(String nodeId) {
        return Collections.unmodifiableList(adjacencyList.getOrDefault(nodeId, Collections.emptyList()));
    }

    public List<Edge> getIncomingEdges(String nodeId) {
        return Collections.unmodifiableList(incomingEdges.getOrDefault(nodeId, Collections.emptyList()));
    }

    @Override
    public long getNodeCount() {
        return nodes.size();
    }

    @Override
    public long getEdgeCount() {
        return adjacencyList.values().stream().mapToLong(List::size).sum();
    }

    public boolean isDirected() {
        return isDirected;
    }
}
