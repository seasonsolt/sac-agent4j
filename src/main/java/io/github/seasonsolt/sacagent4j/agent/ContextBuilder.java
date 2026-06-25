package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.seasonsolt.sacagent4j.state.AgentState;

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

    /** Builds one prompt from a complete run object. */
    public String build(AgentRun run) throws Exception {
        return build(run.task(), run.history(), run.state());
    }

    /** Builds one prompt containing the tool protocol, task, agent state, and prior turns. */
    public String build(String task, List<Turn> history, AgentState agentState) throws Exception {
        StringBuilder context = new StringBuilder();
        context.append("You are sac-agent4j, a minimal SWE agent.\n");
        context.append("Philosophy: one loop, explicit JSON actions, tiny tools, explicit state, verify with tests.\n");
        context.append("Return exactly one JSON object matching one action. No markdown.\n\n");
        context.append("Actions:\n");
        context.append("{\"type\":\"set_plan\",\"items\":[\"inspect failure\",\"patch bug\",\"run tests\"]}\n");
        context.append("{\"type\":\"update_todo\",\"id\":1,\"status\":\"in_progress\"}\n");
        context.append("{\"type\":\"write_virtual_file\",\"path\":\"notes/root-cause.md\",\"content\":\"...\"}\n");
        context.append("{\"type\":\"read_virtual_file\",\"path\":\"notes/root-cause.md\"}\n");
        context.append("{\"type\":\"offload_context\",\"key\":\"test-output\",\"title\":\"full failing test log\",\"content\":\"...\"}\n");
        context.append("{\"type\":\"read_context\",\"key\":\"test-output\"}\n");
        context.append("{\"type\":\"read_file\",\"path\":\"README.md\"}\n");
        context.append("{\"type\":\"search\",\"query\":\"TODO\"}\n");
        context.append("{\"type\":\"shell\",\"command\":\"mvn test\"}\n");
        context.append("{\"type\":\"apply_patch\",\"patch\":\"...unified diff...\"}\n");
        context.append("{\"type\":\"run_tests\"}\n");
        context.append("{\"type\":\"finish\",\"summary\":\"...\"}\n\n");
        context.append("Task:\n").append(task).append("\n\n");
        context.append("Agent state:\n").append(agentState.renderStateSummary()).append("\n");
        context.append("History:\n");
        for (Turn turn : history) {
            context.append(renderTurnForPrompt(turn)).append('\n');
        }
        return context.toString();
    }

    /** Backward-compatible helper for tests that do not care about state. */
    public String build(String task, List<Turn> history) throws Exception {
        return build(task, history, new AgentState());
    }

    private String renderTurnForPrompt(Turn turn) throws Exception {
        if (turn.action() instanceof Action.OffloadContext offloadContext) {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode action = root.putObject("action");
            action.put("type", "offload_context");
            action.put("key", offloadContext.key());
            action.put("title", offloadContext.title());
            action.put("contentChars", offloadContext.content() == null ? 0 : offloadContext.content().length());
            root.set("observation", objectMapper.valueToTree(turn.observation()));
            return objectMapper.writeValueAsString(root);
        }
        return objectMapper.writeValueAsString(turn);
    }
}
