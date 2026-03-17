package com.graphanalytics.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface Graph {

    void addNode(Node node);

    void addEdge(String sourceId, String targetId, double weight);

    void addUndirectedEdge(String sourceId, String targetId, double weight);

    boolean removeNode(String nodeId);

    boolean removeEdge(String sourceId, String targetId);

    Optional<Node> getNode(String id);

    Collection<Node> getNodes();

    List<Edge> getEdges(String nodeId);

    long getNodeCount();

    long getEdgeCount();

}
