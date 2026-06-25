package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

/** Routes a tool action through registry lookup and permission checking. */
public final class ToolActionHandler {
    private final ToolRegistry registry;
    private final PermissionGate permissionGate;

    public ToolActionHandler(ToolRegistry registry, PermissionGate permissionGate) {
        this.registry = registry;
        this.permissionGate = permissionGate;
    }

    public Observation execute(Action.ToolAction action, ToolContext context) throws Exception {
        Tool tool = registry.find(action);
        PermissionDecision decision = permissionGate.check(tool, action, context);
        if (!decision.allowed()) {
            return Observation.failed(decision.reason());
        }
        return tool.execute(action, context);
    }
}
