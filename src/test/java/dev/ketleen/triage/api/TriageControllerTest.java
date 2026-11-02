package dev.ketleen.triage.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ketleen.triage.classifier.TriageClassifier;
import dev.ketleen.triage.model.Category;
import dev.ketleen.triage.model.Severity;
import dev.ketleen.triage.model.TriageResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TriageController.class)
class TriageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TriageClassifier classifier;

    @Test
    void returnsTheClassification() throws Exception {
        when(classifier.classify(any())).thenReturn(
                TriageResult.of(Severity.HIGH, Category.PERFORMANCE, "Export times out",
                        List.of("export"), "llm"));

        mockMvc.perform(post("/api/v1/triage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Slow export\",\"description\":\"It times out\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.category").value("PERFORMANCE"))
                .andExpect(jsonPath("$.suggestedTeam").value("platform"))
                .andExpect(jsonPath("$.source").value("llm"));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/triage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"something\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsTooLongDescription() throws Exception {
        String body = "{\"title\":\"t\",\"description\":\"" + "a".repeat(5001) + "\"}";
        mockMvc.perform(post("/api/v1/triage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
