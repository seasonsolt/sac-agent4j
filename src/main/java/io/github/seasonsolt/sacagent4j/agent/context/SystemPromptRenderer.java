package io.github.seasonsolt.sacagent4j.agent.context;

/** Renders stable system instructions. */
public final class SystemPromptRenderer {
    public String render() {
        return "You are sac-agent4j, a minimal SWE agent.\n"
                + "Philosophy: one loop, explicit JSON actions, tiny tools, explicit state, verify with tests.\n"
                + "Return exactly one JSON object matching one action. No markdown.";
    }
}
