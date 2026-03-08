package com.graphanalytics.algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmResultWrapper<T> {
    private T result;
    private long executionTimeMs;
}
