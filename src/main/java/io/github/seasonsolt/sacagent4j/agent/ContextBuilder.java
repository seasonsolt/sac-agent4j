package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Converts the current task and trajectory into the text prompt seen by the LLM.
 *
 * <p>This class is intentionally simple and inspectable. Later versions can
 * add file-tree summaries, truncation, or prompt templates here without changing
 * the rest of the loop.</p>
 */
public final class ContextBuilder {
    private final ObjectMapper objectMapper;

    public ContextBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Builds one prompt containing the tool protocol, task, and prior turns. */
    public String build(String task, List<Turn> history) throws Exception {
        StringBuilder context = new StringBuilder();
        context.append("You are sac-agent4j, a minimal SWE agent.\n");
        context.append("Philosophy: one loop, explicit JSON actions, tiny tools, verify with tests.\n");
        context.append("Return exactly one JSON object matching one action. No markdown.\n\n");
        context.append("Actions:\n");
        context.append("{\"type\":\"read_file\",\"path\":\"README.md\"}\n");
        context.append("{\"type\":\"search\",\"query\":\"TODO\"}\n");
        context.append("{\"type\":\"shell\",\"command\":\"mvn test\"}\n");
        context.append("{\"type\":\"apply_patch\",\"patch\":\"...unified diff...\"}\n");
        context.append("{\"type\":\"run_tests\"}\n");
        context.append("{\"type\":\"finish\",\"summary\":\"...\"}\n\n");
        context.append("Task:\n").append(task).append("\n\n");
        context.append("History:\n");
        for (Turn turn : history) {
            context.append(objectMapper.writeValueAsString(turn)).append('\n');
        }
        return context.toString();
    }
}
