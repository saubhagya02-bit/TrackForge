package com.trackforge.bug.service;

import com.trackforge.bug.dto.BugDto;
import com.trackforge.bug.entity.Bug;
import com.trackforge.bug.repository.BugRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final BugRepository bugRepository;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.enabled:false}")
    private boolean aiEnabled;

    private static final String OPENAI_CHAT_URL   = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_EMBED_URL  = "https://api.openai.com/v1/embeddings";

    // Public API

    public Map<String, Object> suggestTriageFields(String title, String description) {
        if (aiEnabled && openAiApiKey != null && !openAiApiKey.isBlank()) {
            try {
                return callOpenAiTriage(title, description);
            } catch (Exception e) {
                log.warn("OpenAI triage failed, using heuristics: {}", e.getMessage());
            }
        }
        return heuristicTriage(title, description);
    }

    @Async
    public void processBugEmbedding(UUID bugId) {
        if (!aiEnabled || openAiApiKey == null || openAiApiKey.isBlank()) return;
        try {
            Bug bug = bugRepository.findById(bugId).orElse(null);
            if (bug == null) return;
            String text = bug.getTitle() + " " + (bug.getDescription() != null ? bug.getDescription() : "");
            String embedding = callOpenAiEmbedding(text);
            if (embedding != null) {
                bug.setEmbedding(embedding);
                bugRepository.save(bug);
                log.info("Embedding stored for bug: {}", bugId);
            }
        } catch (Exception e) {
            log.error("Embedding failed for bug {}: {}", bugId, e.getMessage());
        }
    }

    public BugDto.DuplicateCheckResponse findDuplicates(UUID bugId, UUID projectId) {
        try {
            Bug bug = bugRepository.findById(bugId).orElse(null);
            if (bug == null || bug.getEmbedding() == null) {
                return BugDto.DuplicateCheckResponse.builder()
                        .hasDuplicates(false).similarBugs(List.of()).build();
            }
            List<Bug> similar = bugRepository.findSimilarBugs(
                    projectId, bugId, bug.getEmbedding(), 0.85f);
            List<BugDto.Summary> summaries = similar.stream()
                    .map(b -> BugDto.Summary.builder()
                            .id(b.getId()).bugNumber(b.getBugNumber()).title(b.getTitle())
                            .status(b.getStatus().name()).priority(b.getPriority().name())
                            .severity(b.getSeverity().name()).createdAt(b.getCreatedAt()).build())
                    .collect(Collectors.toList());
            return BugDto.DuplicateCheckResponse.builder()
                    .hasDuplicates(!summaries.isEmpty()).similarBugs(summaries).build();
        } catch (Exception e) {
            log.error("Duplicate check failed: {}", e.getMessage());
            return BugDto.DuplicateCheckResponse.builder()
                    .hasDuplicates(false).similarBugs(List.of()).build();
        }
    }

    // OpenAI HTTP calls

    private Map<String, Object> callOpenAiTriage(String title, String description) throws Exception {
        String prompt = String.format(
                "Analyze this bug and respond ONLY with JSON (no markdown):\n" +
                        "{\"priority\":\"CRITICAL|HIGH|MEDIUM|LOW\",\"severity\":\"BLOCKER|CRITICAL|MAJOR|MINOR|TRIVIAL\",\"confidence\":0.0-1.0}\n\n" +
                        "Title: %s\nDescription: %s", title, description != null ? description : "");

        String body = objectMapper.writeValueAsString(Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 100,
                "temperature", 0.1
        ));

        String responseBody = postJson(OPENAI_CHAT_URL, body);
        JsonNode root    = objectMapper.readTree(responseBody);
        String content   = root.path("choices").get(0).path("message").path("content").asText();
        content = content.replaceAll("```json|```", "").trim();

        JsonNode result  = objectMapper.readTree(content);
        Map<String, Object> map = new HashMap<>();
        map.put("priority",   result.path("priority").asText("MEDIUM"));
        map.put("severity",   result.path("severity").asText("MINOR"));
        map.put("confidence", result.path("confidence").floatValue());
        return map;
    }

    private String callOpenAiEmbedding(String text) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "model", "text-embedding-3-small",
                "input", text
        ));
        String responseBody = postJson(OPENAI_EMBED_URL, body);
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("data").get(0).path("embedding").toString();
    }

    private String postJson(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openAiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString())
                .body();
    }

    // Heuristic fallback

    private Map<String, Object> heuristicTriage(String title, String description) {
        String text = (title + " " + (description != null ? description : "")).toLowerCase();
        String priority, severity;

        if (text.matches(".*(crash|down|unavailable|data.loss|security|breach|blocker).*")) {
            priority = "CRITICAL"; severity = "BLOCKER";
        } else if (text.matches(".*(fail|broken|error|exception|cannot|unable|wrong|incorrect).*")) {
            priority = "HIGH";     severity = "MAJOR";
        } else if (text.matches(".*(slow|performance|timeout|delay).*")) {
            priority = "MEDIUM";   severity = "MAJOR";
        } else if (text.matches(".*(ui|display|style|layout|typo|cosmetic|spelling).*")) {
            priority = "LOW";      severity = "TRIVIAL";
        } else {
            priority = "MEDIUM";   severity = "MINOR";
        }

        return Map.of("priority", priority, "severity", severity, "confidence", 0.6f);
    }
}