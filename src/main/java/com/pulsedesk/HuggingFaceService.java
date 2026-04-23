package com.pulsedesk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Service
public class HuggingFaceService {

    @Value("${huggingface.api.token}")
    private String apiToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TicketAnalysis analyzeComment(String content) throws Exception {
        String prompt = """
            Analyze this user comment and decide if it should become a support ticket.
            Comment: "%s"
            
            Priority rules:
            - high: system is broken, user cannot use the app, data loss, security issue
            - medium: feature is degraded but workaround exists, billing issue
            - low: minor UI issue, typo, small suggestion, cosmetic problem
            
            Respond ONLY in JSON format, no extra text:
            {"isTicket": true, "title": "short title", "category": "bug", "priority": "low", "summary": "short summary"}
            category must be one of: bug, feature, billing, account, other
            priority must be one of: low, medium, high
            """.formatted(content);

        String requestBody = objectMapper.writeValueAsString(Map.of(
            "model", "Qwen/Qwen2.5-7B-Instruct",
            "messages", new Object[]{
                Map.of("role", "user", "content", prompt)
            },
            "max_tokens", 200,
            "stream", false
        ));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://router.huggingface.co/v1/chat/completions"))
            .header("Authorization", "Bearer " + apiToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("HF Response: " + response.body());

        return parseResponse(response.body());
    }

    private TicketAnalysis parseResponse(String responseBody) {
        try {
            var root = objectMapper.readTree(responseBody);
            String text = root.get("choices").get(0).get("message").get("content").asText();
            int start = text.indexOf("{");
            int end = text.lastIndexOf("}") + 1;
            if (start == -1 || end == 0) return fallbackAnalysis();

            String json = text.substring(start, end);
            return objectMapper.readValue(json, TicketAnalysis.class);
        } catch (Exception e) {
            System.out.println("Parse error: " + e.getMessage());
            return fallbackAnalysis();
        }
    }

    private TicketAnalysis fallbackAnalysis() {
        TicketAnalysis t = new TicketAnalysis();
        t.setIsTicket(false);
        return t;
    }
}