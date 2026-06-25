package io.github.seasonsolt.sacagent4j.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.github.seasonsolt.sacagent4j.agent.Action;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiCompatibleLlmClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesOpenAiCompatibleChatCompletionIntoAction() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String body = """
                    {"choices":[{"message":{"content":"{\\\"type\\\":\\\"finish\\\",\\\"summary\\\":\\\"done\\\"}"}}]}
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                    objectMapper,
                    HttpClient.newHttpClient(),
                    baseUrl,
                    "test-key",
                    "test-model"
            );

            Action action = client.nextAction("context");

            Action.Finish finish = assertInstanceOf(Action.Finish.class, action);
            assertEquals("done", finish.summary());
            assertEquals("Bearer test-key", authorization.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsMissingApiKey() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                new OpenAiCompatibleLlmClient(objectMapper, HttpClient.newHttpClient(), URI.create("http://localhost/v1"), "", "model")
        );
        assertEquals("apiKey is required for OpenAI-compatible mode", error.getMessage());
    }
}
