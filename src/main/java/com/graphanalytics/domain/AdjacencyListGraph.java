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
            Node source = nodes.get(sourceId);
            Node target = nodes.get(targetId);

            if (source == null || target == null) {
                throw new IllegalArgumentException("Source or target node does not exist in the graph");
            }

            Edge forward = new Edge(source, target, weight);
            Edge reverse = new Edge(target, source, weight);

            adjacencyList.get(sourceId).add(forward);
            incomingEdges.get(targetId).add(forward);

            adjacencyList.get(targetId).add(reverse);
            incomingEdges.get(sourceId).add(reverse);
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
    public boolean removeNode(String nodeId) {
        lock.writeLock().lock();
        try {
            if (!nodes.containsKey(nodeId)) return false;

            // Remove all edges from/to this node
            adjacencyList.getOrDefault(nodeId, Collections.emptyList()).clear();
            incomingEdges.getOrDefault(nodeId, Collections.emptyList()).clear();

            // Remove edges referencing this node from other lists
            for (List<Edge> edgeList : adjacencyList.values()) {
                edgeList.removeIf(e -> e.getTarget().getId().equals(nodeId));
            }
            for (List<Edge> edgeList : incomingEdges.values()) {
                edgeList.removeIf(e -> e.getSource().getId().equals(nodeId));
            }

            adjacencyList.remove(nodeId);
            incomingEdges.remove(nodeId);
            nodes.remove(nodeId);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeEdge(String sourceId, String targetId) {
        lock.writeLock().lock();
        try {
            List<Edge> outEdges = adjacencyList.get(sourceId);
            List<Edge> inEdges = incomingEdges.get(targetId);
            if (outEdges == null || inEdges == null) return false;

            boolean removed = outEdges.removeIf(e -> e.getTarget().getId().equals(targetId));
            inEdges.removeIf(e -> e.getSource().getId().equals(sourceId));
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<Edge> getEdges(String nodeId) {
        lock.readLock().lock();
        try {
            List<Edge> edges = adjacencyList.get(nodeId);
            return edges == null ? Collections.emptyList() : Collections.unmodifiableList(edges);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Edge> getIncomingEdges(String nodeId) {
        lock.readLock().lock();
        try {
            List<Edge> edges = incomingEdges.get(nodeId);
            return edges == null ? Collections.emptyList() : Collections.unmodifiableList(edges);
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
