package io.github.seasonsolt.sacagent4j.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.seasonsolt.sacagent4j.agent.Action;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal OpenAI-compatible chat-completions adapter.
 *
 * <p>It deliberately speaks the common {@code /chat/completions} shape instead
 * of depending on a vendor SDK. That keeps the MVP small and lets users point it
 * at OpenAI, local gateways, or OpenAI-compatible providers by changing the base
 * URL.</p>
 */
public final class OpenAiCompatibleLlmClient implements LlmClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI chatCompletionsUri;
    private final String apiKey;
    private final String model;

    public OpenAiCompatibleLlmClient(ObjectMapper objectMapper, HttpClient httpClient, URI baseUrl, String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey is required for OpenAI-compatible mode");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model is required for OpenAI-compatible mode");
        }
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.chatCompletionsUri = normalizeBaseUrl(baseUrl).resolve("chat/completions");
        this.apiKey = apiKey;
        this.model = model;
    }

    /** Creates a client from conventional environment variables. */
    public static OpenAiCompatibleLlmClient fromEnv(ObjectMapper objectMapper, String modelOverride) {
        String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1");
        String apiKey = System.getenv("OPENAI_API_KEY");
        String model = modelOverride == null || modelOverride.isBlank()
                ? System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini")
                : modelOverride;
        return new OpenAiCompatibleLlmClient(
                objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build(),
                URI.create(baseUrl),
                apiKey,
                model
        );
    }

    @Override
    public Action nextAction(String context) throws Exception {
        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("model", model);
        requestJson.put("temperature", 0);

        ArrayNode messages = requestJson.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", "Return exactly one JSON action object. Do not wrap it in markdown.");
        messages.addObject()
                .put("role", "user")
                .put("content", context);

        HttpRequest request = HttpRequest.newBuilder(chatCompletionsUri)
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestJson)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("LLM HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText();
        return objectMapper.readValue(stripJsonFence(content), Action.class);
    }

    private static URI normalizeBaseUrl(URI baseUrl) {
        String value = baseUrl.toString();
        if (!value.endsWith("/")) {
            value = value + "/";
        }
        return URI.create(value);
    }

    /** Tolerates providers that still wrap JSON in a markdown code fence. */
    private static String stripJsonFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }
}
