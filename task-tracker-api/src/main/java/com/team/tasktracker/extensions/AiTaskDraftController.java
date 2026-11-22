package com.team.tasktracker.extensions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/tasks")
public class AiTaskDraftController {

    private final boolean aiEnabled;
    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public AiTaskDraftController(
            @Value("${app.ai.enabled:false}") boolean aiEnabled,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:gemini-2.5-flash}") String model
    ) {
        this.aiEnabled = aiEnabled;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.create();
    }

    public record DraftRequest(
            @NotBlank @Size(max = 2000) String prompt
    ) {}

    @PostMapping("/draft")
    public ResponseEntity<Map<String, String>> draftTask(@Valid @RequestBody DraftRequest req) {
        String cleanPrompt = req.prompt().trim();
        if (!aiEnabled || apiKey == null || apiKey.isBlank()) {
            String summary = cleanPrompt.length() > 140 ? cleanPrompt.substring(0, 137) + "..." : cleanPrompt;
            return ResponseEntity.ok(Map.of(
                    "title", summary,
                    "description", "Objective: " + cleanPrompt + "\n\nAcceptance Criteria:\n- Implementation verified\n- Tests and documentation updated",
                    "summary", summary,
                    "mode", "fallback-template"
            ));
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of(
                                "text", "Turn this rough task note into a concise task summary and structured description: " + cleanPrompt
                        ))
                ))
        );

        try {
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return ResponseEntity.ok(Map.of(
                    "summary", cleanPrompt.length() > 140 ? cleanPrompt.substring(0, 137) + "..." : cleanPrompt,
                    "description", response != null ? response.toString() : cleanPrompt,
                    "mode", "gemini"
            ));
        } catch (Exception ex) {
            return ResponseEntity.ok(Map.of(
                    "summary", cleanPrompt.length() > 140 ? cleanPrompt.substring(0, 137) + "..." : cleanPrompt,
                    "description", cleanPrompt,
                    "mode", "fallback-on-error"
            ));
        }
    }
}
