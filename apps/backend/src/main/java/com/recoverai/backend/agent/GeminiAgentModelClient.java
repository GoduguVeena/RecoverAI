package com.recoverai.backend.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.dto.AgentRequestContext;
import com.recoverai.backend.dto.AgentRecommendationResponse;
import com.recoverai.backend.exception.AgentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Primary
public class GeminiAgentModelClient implements AgentModelClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiAgentModelClient.class);

    private final RestTemplate restTemplate;
    private final FakeAgentModelClient fakeAgentModelClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GeminiAgentModelClient(RestTemplate restTemplate,
                                  FakeAgentModelClient fakeAgentModelClient,
                                  @Value("${gemini.api-key:${GEMINI_API_KEY:}}") String apiKey) {
        this.restTemplate = restTemplate;
        this.fakeAgentModelClient = fakeAgentModelClient;
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
    }

    @Override
    public AgentRecommendationResponse generateRecommendation(AgentRequestContext context) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("GEMINI_API_KEY is not configured. Falling back to FakeAgentModelClient.");
            return fakeAgentModelClient.generateRecommendation(context);
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

            String promptText = String.format(
                    "You are a payment recovery recommendation agent for RecoverAI.\n" +
                    "Analyze this payment failure context and recommend a recovery action:\n" +
                    "- Merchant Category: %s\n" +
                    "- Customer Segment: %s\n" +
                    "- Payment Amount: %s %s\n" +
                    "- Payment Method: %s\n" +
                    "- Failure Code: %s\n" +
                    "- Failure Reason: %s\n" +
                    "- Retry Count: %d\n" +
                    "- Customer Transactions: %d (Successes: %d, Failures: %d)\n" +
                    "- Success Rate: %s\n" +
                    "- Days Since Last Success: %s\n" +
                    "- ML Recovery Probability: %s\n" +
                    "- ML Model Version: %s\n\n" +
                    "Respond with ONLY a JSON object in this exact schema:\n" +
                    "{\n" +
                    "  \"modelVersion\": \"%s\",\n" +
                    "  \"recoveryProbability\": %s,\n" +
                    "  \"diagnosis\": \"Short diagnostic description\",\n" +
                    "  \"candidateActions\": [\"RETRY\", \"PAYMENT_LINK\", \"NOTIFICATION\"],\n" +
                    "  \"selectedAction\": \"RETRY\",\n" +
                    "  \"reasoningSummary\": \"Concise operational explanation\"\n" +
                    "}\n" +
                    "Constraints: selectedAction MUST be one of RETRY, PAYMENT_LINK, NOTIFICATION. No markdown formatting outside JSON.",
                    context.getMerchantCategory(), context.getCustomerSegment(),
                    context.getAmount(), context.getCurrency(), context.getPaymentMethod(),
                    context.getFailureCode(), context.getFailureReason(),
                    context.getRetryCount(), context.getCustomerTotalTransactions(),
                    context.getCustomerSuccessfulTransactions(), context.getCustomerFailedTransactions(),
                    context.getCustomerSuccessRate(), context.getDaysSinceLastSuccess(),
                    context.getRecoveryProbability(), context.getModelVersion(),
                    context.getModelVersion(), context.getRecoveryProbability()
            );

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", promptText)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "responseMimeType", "application/json"
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            return parseGeminiResponse(responseStr, context);
        } catch (Exception e) {
            log.warn("Gemini API call failed ({}); falling back safely to FakeAgentModelClient.", e.getMessage());
            return fakeAgentModelClient.generateRecommendation(context);
        }
    }

    private AgentRecommendationResponse parseGeminiResponse(String rawResponse, AgentRequestContext context) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String textContent = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Strip markdown block ticks if present
            String jsonClean = textContent.trim();
            if (jsonClean.startsWith("```json")) {
                jsonClean = jsonClean.substring(7);
            }
            if (jsonClean.startsWith("```")) {
                jsonClean = jsonClean.substring(3);
            }
            if (jsonClean.endsWith("```")) {
                jsonClean = jsonClean.substring(0, jsonClean.length() - 3);
            }
            jsonClean = jsonClean.trim();

            JsonNode node = objectMapper.readTree(jsonClean);

            String modelVersion = node.has("modelVersion") ? node.get("modelVersion").asText() : context.getModelVersion();
            BigDecimal probability = node.has("recoveryProbability") ? new BigDecimal(node.get("recoveryProbability").asText()) : context.getRecoveryProbability();
            String diagnosis = node.has("diagnosis") ? node.get("diagnosis").asText() : "Diagnosis unavailable";
            String selectedActionStr = node.has("selectedAction") ? node.get("selectedAction").asText() : "RETRY";
            RecoveryActionType selectedAction = RecoveryActionType.valueOf(selectedActionStr.toUpperCase());
            String reasoningSummary = node.has("reasoningSummary") ? node.get("reasoningSummary").asText() : "Reasoning summary unavailable";

            List<RecoveryActionType> candidateActions = new ArrayList<>();
            if (node.has("candidateActions") && node.get("candidateActions").isArray()) {
                for (JsonNode actionNode : node.get("candidateActions")) {
                    candidateActions.add(RecoveryActionType.valueOf(actionNode.asText().toUpperCase()));
                }
            } else {
                candidateActions = List.of(RecoveryActionType.RETRY, RecoveryActionType.PAYMENT_LINK, RecoveryActionType.NOTIFICATION);
            }

            return new AgentRecommendationResponse(modelVersion, probability, diagnosis, candidateActions, selectedAction, reasoningSummary);
        } catch (Exception e) {
            log.error("Failed to parse Gemini response text: ", e);
            throw new AgentException("Failed to parse Gemini agent response JSON", e);
        }
    }
}
