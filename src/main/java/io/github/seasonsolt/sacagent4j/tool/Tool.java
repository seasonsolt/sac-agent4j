package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

/** One executable workspace capability. */
public interface Tool {
    String name();

    RiskLevel risk();

    boolean supports(Action.ToolAction action);

    Observation execute(Action.ToolAction action, ToolContext context) throws Exception;
}
