package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.context.DefaultContextManager;
import io.github.seasonsolt.sacagent4j.agent.context.Prompt;
import io.github.seasonsolt.sacagent4j.state.AgentState;

import java.util.List;

/**
 * Backward-compatible facade over {@link DefaultContextManager}.
 *
 * <p>The prompt system now has a first-class {@code ContextManager} seam and
 * structured {@code Prompt}. This class remains so older tests/callers can still
 * request a rendered string directly.</p>
 */
public final class ContextBuilder {
    private final DefaultContextManager contextManager;

    public ContextBuilder(ObjectMapper objectMapper) {
        this.contextManager = new DefaultContextManager(objectMapper);
    }

    /** Builds one prompt from a complete run object. */
    public String build(AgentRun run) throws Exception {
        return contextManager.buildPrompt(run).render();
    }

    /** Builds one prompt containing the tool protocol, task, agent state, and prior turns. */
    public String build(String task, List<Turn> history, AgentState agentState) throws Exception {
        AgentRun run = AgentRun.start(task, Math.max(1, history.size() + 1));
        for (Turn turn : history) {
            run.record(turn.action(), turn.observation());
        }
        Prompt basePrompt = contextManager.buildPrompt(run);
        Prompt prompt = new Prompt(
                basePrompt.system(),
                basePrompt.actionProtocol(),
                "Task:\n" + task + "\n",
                "Agent state:\n" + agentState.renderStateSummary(),
                basePrompt.history()
        );
        return prompt.render();
    }

    /** Backward-compatible helper for tests that do not care about state. */
    public String build(String task, List<Turn> history) throws Exception {
        return build(task, history, new AgentState());
    }
}
