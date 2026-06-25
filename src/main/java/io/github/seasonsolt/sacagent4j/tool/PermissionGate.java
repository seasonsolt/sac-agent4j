package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;

/** Boundary for approving, editing, or rejecting risky tool actions. */
public interface PermissionGate {
    PermissionDecision check(Tool tool, Action.ToolAction action, ToolContext context);
}
