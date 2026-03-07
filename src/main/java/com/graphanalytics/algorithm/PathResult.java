package com.graphanalytics.algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PathResult {
    private List<String> pathNodeIds;
    private double totalCost;
}
