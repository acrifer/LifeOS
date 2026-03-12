package com.lifeos.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.config.AiProperties;
import com.lifeos.ai.service.AiSummaryService;
import com.lifeos.api.ai.client.NoteFeignClient;
import com.lifeos.api.ai.dto.AiSummaryCommand;
import com.lifeos.api.ai.dto.AiSummaryResult;
import com.lifeos.api.ai.dto.NoteInternalUpdateSummaryDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class AiSummaryServiceImpl implements AiSummaryService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    @Resource
    private NoteFeignClient noteFeignClient;

    @Resource
    private AiProperties aiProperties;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public AiSummaryResult generateAndWriteBack(AiSummaryCommand command) {
        AiSummaryResult result = new AiSummaryResult();
        result.setNoteId(command.getNoteId());
        try {
            String summary = generateSummary(command);
            NoteInternalUpdateSummaryDTO updateRequest = new NoteInternalUpdateSummaryDTO();
            updateRequest.setUserId(command.getUserId());
            updateRequest.setSummary(summary);
            noteFeignClient.updateSummary(command.getNoteId(), updateRequest);
            result.setSummary(summary);
            result.setStatus(STATUS_SUCCESS);
        } catch (Exception ex) {
            log.warn("Failed to generate note summary for note {}", command.getNoteId(), ex);
            result.setStatus(STATUS_FAILED);
            result.setErrorMessage(ex.getMessage());
        }
        return result;
    }

    private String generateSummary(AiSummaryCommand command) throws Exception {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            return buildMockSummary(command);
        }

        RestClient client = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", aiProperties.getModel());
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You summarize user notes into concise Chinese bullet-style prose."),
                Map.of("role", "user", "content", buildPrompt(command))));
        requestBody.put("temperature", 0.3);

        String response = client.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (!contentNode.isTextual() || !StringUtils.hasText(contentNode.asText())) {
            throw new RuntimeException("AI response did not contain summary content");
        }
        return contentNode.asText().trim();
    }

    private String buildMockSummary(AiSummaryCommand command) {
        String title = StringUtils.hasText(command.getTitle()) ? command.getTitle().trim() : "Untitled";
        String content = StringUtils.hasText(command.getContent()) ? command.getContent().trim() : "";
        String normalized = content.replaceAll("\\s+", " ");
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 120) + "...";
        }
        if (!StringUtils.hasText(normalized)) {
            normalized = "This note is currently empty and is waiting for more detailed content.";
        }
        return String.format(Locale.ROOT, "Summary of \"%s\": %s", title, normalized);
    }

    private String buildPrompt(AiSummaryCommand command) {
        String title = StringUtils.hasText(command.getTitle()) ? command.getTitle() : "Untitled";
        String content = StringUtils.hasText(command.getContent()) ? command.getContent() : "";
        return "Please summarize the following note in concise Chinese.\n"
                + "Title: " + title + "\n"
                + "Content:\n" + content + "\n"
                + "Requirements: keep it short, clear, and useful.";
    }
}
