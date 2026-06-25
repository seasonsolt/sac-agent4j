package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;

/**
 * Non-interactive permission gate for the MVP.
 *
 * <p>It allows low-risk actions and applies the current shell policy to
 * medium/high-risk command-like actions. A later HITL gate can implement the
 * same port.</p>
 */
public final class DefaultPermissionGate implements PermissionGate {
    @Override
    public PermissionDecision check(Tool tool, Action.ToolAction action, ToolContext context) {
        return switch (tool.risk()) {
            case LOW -> PermissionDecision.allow();
            case MEDIUM -> checkMediumRisk(action, context);
            case HIGH -> checkHighRisk(action, context);
        };
    }

    private PermissionDecision checkMediumRisk(Action.ToolAction action, ToolContext context) {
        if (action instanceof Action.RunTests) {
            return context.toolPolicy().checkShell(context.testCommand());
        }
        return PermissionDecision.allow();
    }

    private PermissionDecision checkHighRisk(Action.ToolAction action, ToolContext context) {
        if (action instanceof Action.Shell shell) {
            return context.toolPolicy().checkShell(shell.command());
        }
        return PermissionDecision.deny("high-risk tool requires explicit policy handling: " + action.getClass().getSimpleName());
    }
}
