package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;

/**
 * Non-interactive permission gate for the MVP.
 *
 * <p>It allows ordinary low/medium-risk actions and applies the current shell
 * policy to shell-like actions. A later HITL gate can implement the same port.</p>
 */
public final class DefaultPermissionGate implements PermissionGate {
    @Override
    public PermissionDecision check(Tool tool, Action.ToolAction action, ToolContext context) {
        if (action instanceof Action.Shell shell) {
            PolicyDecision decision = context.toolPolicy().checkShell(shell.command());
            return decision.allowed() ? PermissionDecision.allow() : PermissionDecision.deny(decision.reason());
        }
        if (action instanceof Action.RunTests) {
            PolicyDecision decision = context.toolPolicy().checkShell(context.testCommand());
            return decision.allowed() ? PermissionDecision.allow() : PermissionDecision.deny(decision.reason());
        }
        return PermissionDecision.allow();
    }
}
