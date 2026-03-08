package com.graphanalytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphanalytics.service.GraphService.EdgeDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GraphControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @SuppressWarnings("null")
        void testCreateGraphAndRunDijkstra() throws Exception {
                // 1. Create Graph
                String responseContent = mockMvc.perform(post("/api/graphs?directed=false"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").exists())
                                .andReturn().getResponse().getContentAsString();

                String graphId = objectMapper.readTree(responseContent).get("id").asText();

                // 2. Add Edges
                EdgeDto e1 = new EdgeDto();
                e1.setSource("A");
                e1.setTarget("B");
                e1.setWeight(1.0);
                EdgeDto e2 = new EdgeDto();
                e2.setSource("B");
                e2.setTarget("C");
                e2.setWeight(2.0);
                EdgeDto e3 = new EdgeDto();
                e3.setSource("A");
                e3.setTarget("C");
                e3.setWeight(4.0);
                List<EdgeDto> edges = List.of(e1, e2, e3);

                String jsonContent = objectMapper.writeValueAsString(edges);
                if (jsonContent != null) {
                        mockMvc.perform(post("/api/graphs/{id}/edges", graphId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(jsonContent))
                                        .andExpect(status().isOk());
                }

                // 3. Run Dijkstra
                mockMvc.perform(post("/api/graphs/{id}/algorithms/shortest-path/dijkstra", graphId)
                                .param("source", "A")
                                .param("target", "C"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.result.totalCost").value(3.0))
                                .andExpect(jsonPath("$.result.pathNodeIds[0]").value("A"))
                                .andExpect(jsonPath("$.result.pathNodeIds[1]").value("B"))
                                .andExpect(jsonPath("$.result.pathNodeIds[2]").value("C"))
                                .andExpect(jsonPath("$.executionTimeMs").exists());
        }
}
