package io.github.seasonsolt.sacagent4j.agent;

import java.util.List;

/** Final outcome of an agent run plus the trajectory needed for debugging. */
public record AgentResult(boolean finished, String summary, List<Turn> history) {
    public static AgentResult finished(String summary, List<Turn> history) {
        return new AgentResult(true, summary, List.copyOf(history));
    }

    public static AgentResult stopped(List<Turn> history) {
        return new AgentResult(false, "stopped after max steps", List.copyOf(history));
    }
}
