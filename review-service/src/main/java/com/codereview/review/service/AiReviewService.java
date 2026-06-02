package com.codereview.review.service;

import com.codereview.review.dto.ReviewDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiReviewService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Groq API — OpenAI-compatible endpoint
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    public AiReviewResult analyzeCode(String codeSnippet, String language) {
        if (codeSnippet == null || codeSnippet.isBlank()) {
            return fallbackResponse("Code snippet must not be empty.");
        }
        if (language == null || language.isBlank()) {
            return fallbackResponse("Language must not be empty.");
        }
        if (codeSnippet.length() > 50_000) {
            return fallbackResponse("Code snippet exceeds maximum allowed length of 50,000 characters.");
        }

        // FIX: Validate API key is configured before making the request
        if (groqApiKey == null || groqApiKey.isBlank() || groqApiKey.equals("YOUR_GROQ_API_KEY_HERE")) {
            return fallbackResponse("Groq API key is not configured. Please set it in application.yml.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            String systemPrompt =
                "You are a strict expert code reviewer. " +
                "Actively look for bugs, null pointer exceptions, security vulnerabilities, " +
                "missing error handling, logic flaws, off-by-one errors, and bad practices. " +
                "Do NOT say code is correct unless you are absolutely certain. " +
                "Always respond with valid JSON only. No markdown fences, no explanation outside JSON.";

            String userPrompt = buildPrompt(codeSnippet, language);

            Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user",   "content", userPrompt)
                ),
                "temperature", 0.2,
                "max_tokens", 2048
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Sending code review request to Groq API for language: {}", language);
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, entity, String.class);
            log.info("Groq API response status: {}", response.getStatusCode());

            return parseGroqResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            log.error("Groq API client error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return fallbackResponse("Groq API rejected the request (" + e.getStatusCode() + "). Check your API key.");
        } catch (HttpServerErrorException e) {
            log.error("Groq API server error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return fallbackResponse("Groq API is temporarily unavailable. Please retry later.");
        } catch (Exception e) {
            log.error("Groq AI review failed: {}", e.getMessage(), e);
            return fallbackResponse(e.getMessage());
        }
    }

    private String buildPrompt(String codeSnippet, String language) {
        // FIX: substring was using original language.length() on the already-sanitized string,
        // which causes StringIndexOutOfBoundsException when sanitization removes characters.
        // Now we correctly use safeLanguage.length() after sanitizing.
        String safeLanguage = language.replaceAll("[^a-zA-Z0-9+#\\-_]", "");
        safeLanguage = safeLanguage.substring(0, Math.min(safeLanguage.length(), 30));

        return String.format("""
            Review the following %s code and respond ONLY with a JSON object in this exact format:
            {
              "overallScore": <integer 0-100>,
              "summary": "<2-3 sentence overall assessment>",
              "issues": [
                {
                  "severity": "<HIGH|MEDIUM|LOW>",
                  "type": "<BUG|PERFORMANCE|SECURITY|STYLE|MAINTAINABILITY>",
                  "description": "<clear description of the issue>",
                  "lineHint": "<brief hint about where the issue is>"
                }
              ],
              "suggestions": [
                "<actionable improvement suggestion 1>",
                "<actionable improvement suggestion 2>",
                "<actionable improvement suggestion 3>"
              ]
            }

            Code to review:
            ```%s
            %s
            ```

            Respond with ONLY the JSON object. No markdown fences. No extra text.
            """, safeLanguage, safeLanguage, codeSnippet);
    }

    private AiReviewResult parseGroqResponse(String responseBody) throws Exception {
        log.debug("Raw Groq response: {}", responseBody);

        if (responseBody == null || responseBody.isBlank()) {
            throw new Exception("Empty response body from Groq API.");
        }

        JsonNode root = objectMapper.readTree(responseBody);

        // Groq uses OpenAI format: choices[0].message.content
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new Exception("Groq response missing 'choices'. Raw: " + responseBody);
        }

        String content = choices.get(0)
                .path("message")
                .path("content")
                .asText("").trim();

        if (content.isEmpty()) {
            throw new Exception("Groq returned empty content. Raw: " + responseBody);
        }

        // Strip markdown fences if model accidentally added them
        content = content.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").trim();

        // FIX: extract only the JSON object in case the model added extra text before/after
        int jsonStart = content.indexOf('{');
        int jsonEnd = content.lastIndexOf('}');
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            content = content.substring(jsonStart, jsonEnd + 1);
        }

        log.debug("Extracted Groq content: {}", content);

        JsonNode parsed = objectMapper.readTree(content);

        AiReviewResult result = new AiReviewResult();
        result.setOverallScore(parsed.path("overallScore").asInt(50));
        result.setSummary(parsed.path("summary").asText("Code review completed."));
        result.setRawFeedback(content);

        List<ReviewDto.Issue> issues = new ArrayList<>();
        JsonNode issuesNode = parsed.path("issues");
        if (issuesNode.isArray()) {
            for (JsonNode issueNode : issuesNode) {
                ReviewDto.Issue issue = new ReviewDto.Issue();
                issue.setSeverity(issueNode.path("severity").asText("LOW"));
                issue.setType(issueNode.path("type").asText("STYLE"));
                issue.setDescription(issueNode.path("description").asText());
                issue.setLineHint(issueNode.path("lineHint").asText());
                issues.add(issue);
            }
        }
        result.setIssues(issues);

        List<String> suggestions = new ArrayList<>();
        JsonNode suggestionsNode = parsed.path("suggestions");
        if (suggestionsNode.isArray()) {
            for (JsonNode s : suggestionsNode) {
                suggestions.add(s.asText());
            }
        }
        result.setSuggestions(suggestions);

        return result;
    }

    private AiReviewResult fallbackResponse(String errorMessage) {
        AiReviewResult result = new AiReviewResult();
        result.setOverallScore(0);
        result.setSummary("AI analysis could not be completed. Error: " + errorMessage);
        result.setIssues(new ArrayList<>());
        result.setSuggestions(List.of(
                "Check that your Groq API key is set correctly in application.yml",
                "Ensure the key is from Groq Console (console.groq.com)",
                "Verify network connectivity to api.groq.com"
        ));
        result.setRawFeedback("{}");
        return result;
    }

    public static class AiReviewResult {
        private int overallScore;
        private String summary;
        private List<ReviewDto.Issue> issues;
        private List<String> suggestions;
        private String rawFeedback;

        public int getOverallScore() { return overallScore; }
        public void setOverallScore(int v) { this.overallScore = v; }
        public String getSummary() { return summary; }
        public void setSummary(String v) { this.summary = v; }
        public List<ReviewDto.Issue> getIssues() { return issues; }
        public void setIssues(List<ReviewDto.Issue> v) { this.issues = v; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> v) { this.suggestions = v; }
        public String getRawFeedback() { return rawFeedback; }
        public void setRawFeedback(String v) { this.rawFeedback = v; }
    }
}
