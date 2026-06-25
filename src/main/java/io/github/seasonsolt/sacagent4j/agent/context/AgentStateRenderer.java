package io.github.seasonsolt.sacagent4j.agent.context;

import io.github.seasonsolt.sacagent4j.state.AgentState;

/** Renders the agent's inner world without exposing bulky offloaded content. */
public final class AgentStateRenderer {
    public String render(AgentState state) {
        StringBuilder out = new StringBuilder();
        out.append("Agent state:\n");
        out.append("Current plan:\n").append(state.todoList().render()).append('\n');
        out.append("Virtual files:\n");
        if (state.virtualFileSummary().isEmpty()) {
            out.append("No virtual files. Use write_virtual_file for notes or drafts.\n");
        } else {
            state.virtualFileSummary().forEach((path, chars) -> out.append("- ").append(path).append(" (").append(chars).append(" chars)\n"));
        }
        out.append("Context offloads:\n");
        if (state.contextSummary().isEmpty()) {
            out.append("No offloaded context. Use offload_context for bulky snippets.\n");
        } else {
            state.contextSummary().forEach((key, summary) -> out.append("- ").append(key).append(": ").append(summary).append('\n'));
        }
        return out.toString();
    }
}
