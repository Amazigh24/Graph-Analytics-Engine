package com.graphanalytics.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Graph Analytics Engine API",
                description = "REST API for graph creation, algorithm execution, and persistence",
                version = "1.0.0"
        ),
        tags = {
                @Tag(name = "Graph Management", description = "Create graphs, add edges, and query nodes/edges"),
                @Tag(name = "Algorithms", description = "Execute graph algorithms (PageRank, shortest path, traversal, centrality, community detection)"),
                @Tag(name = "Persistence", description = "Save and load graphs to/from files")
        }
)
public class OpenApiConfig {
}
