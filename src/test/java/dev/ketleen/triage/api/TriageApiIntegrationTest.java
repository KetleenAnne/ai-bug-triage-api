package dev.ketleen.triage.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Full application context with the default (offline, rule-based) provider. */
@SpringBootTest
@AutoConfigureMockMvc
class TriageApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void triagesEndToEndWithoutAnyApiKey() throws Exception {
        mockMvc.perform(post("/api/v1/triage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Users cannot login after deploy\","
                                + "\"description\":\"Everyone is rejected\",\"environment\":\"production\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.suggestedTeam").value("backend"))
                .andExpect(jsonPath("$.source").value("rules"));
    }
}
