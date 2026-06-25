package io.github.seasonsolt.sacagent4j.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.Action;

import java.io.BufferedReader;
import java.io.PrintStream;

public final class JsonLineLlmClient implements LlmClient {
    private final ObjectMapper objectMapper;
    private final BufferedReader input;
    private final PrintStream output;

    public JsonLineLlmClient(ObjectMapper objectMapper, BufferedReader input, PrintStream output) {
        this.objectMapper = objectMapper;
        this.input = input;
        this.output = output;
    }

    @Override
    public Action nextAction(String context) throws Exception {
        output.println("--- context ---");
        output.println(context);
        output.println("--- enter one JSON action ---");
        String line = input.readLine();
        if (line == null || line.isBlank()) {
            return new Action.Finish("no action provided");
        }
        return objectMapper.readValue(line, Action.class);
    }
}
