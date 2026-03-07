package com.graphanalytics.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class Edge {
    private Node source;
    private Node target;
    private double weight;
}
