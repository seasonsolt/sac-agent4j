package io.github.seasonsolt.sacagent4j.agent.context;

import io.github.seasonsolt.sacagent4j.state.AgentState;

/** Renders the agent's inner world without exposing bulky offloaded content. */
public final class AgentStateRenderer {
    public String render(AgentState state) {
        return "Agent state:\n" + state.renderStateSummary();
    }
}
