package com.graphanalytics.domain;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AdjacencyListGraph implements Graph {

    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private final Map<String, List<Edge>> adjacencyList = new ConcurrentHashMap<>();

    // For fast retrieval of incoming edges in certain algorithms
    private final Map<String, List<Edge>> incomingEdges = new ConcurrentHashMap<>();

    private boolean isDirected;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public AdjacencyListGraph(boolean isDirected) {
        this.isDirected = isDirected;
    }

    @Override
    public void addNode(Node node) {
        lock.writeLock().lock();
        try {
            nodes.putIfAbsent(node.getId(), node);
            adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
            incomingEdges.putIfAbsent(node.getId(), new ArrayList<>());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addEdge(String sourceId, String targetId, double weight) {
        lock.writeLock().lock();
        try {
            Node source = nodes.get(sourceId);
            Node target = nodes.get(targetId);

            if (source == null || target == null) {
                throw new IllegalArgumentException("Source or target node does not exist in the graph");
            }

            Edge edge = new Edge(source, target, weight);
            adjacencyList.get(sourceId).add(edge);
            incomingEdges.get(targetId).add(edge);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addUndirectedEdge(String sourceId, String targetId, double weight) {
        lock.writeLock().lock();
        try {
            addEdge(sourceId, targetId, weight);
            addEdge(targetId, sourceId, weight);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Node> getNode(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(nodes.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<Node> getNodes() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(new ArrayList<>(nodes.values()));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Edge> getEdges(String nodeId) {
        lock.readLock().lock();
        try {
            List<Edge> edges = adjacencyList.get(nodeId);
            return edges == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(edges));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Edge> getIncomingEdges(String nodeId) {
        lock.readLock().lock();
        try {
            List<Edge> edges = incomingEdges.get(nodeId);
            return edges == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(edges));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public long getNodeCount() {
        lock.readLock().lock();
        try {
            return nodes.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public long getEdgeCount() {
        lock.readLock().lock();
        try {
            return adjacencyList.values().stream().mapToLong(List::size).sum();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isDirected() {
        return isDirected;
    }
}
