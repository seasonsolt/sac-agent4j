package io.github.seasonsolt.sacagent4j.agent.context;

/** Structured prompt assembled from independently testable sections. */
public record Prompt(String system, String actionProtocol, String task, String agentState, String history) {
    /** Renders the prompt into the plain string expected by current LLM clients. */
    public String render() {
        return system
                + "\n\n"
                + actionProtocol
                + "\n"
                + task
                + "\n"
                + agentState
                + "\n"
                + history;
    }
}
