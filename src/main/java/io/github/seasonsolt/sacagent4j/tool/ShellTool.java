package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.time.Duration;

/** Runs a shell command in the workspace after permission approval. */
public final class ShellTool implements Tool {
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    @Override public String name() { return "shell"; }
    @Override public RiskLevel risk() { return RiskLevel.HIGH; }
    @Override public boolean supports(Action.ToolAction action) { return action instanceof Action.Shell; }

    @Override
    public Observation execute(Action.ToolAction action, ToolContext context) throws Exception {
        Action.Shell shell = (Action.Shell) action;
        return runShell(context, shell.command());
    }

    static Observation runShell(ToolContext context, String command) throws Exception {
        return ProcessRunner.runShell(context.workspace().root(), command, DEFAULT_TIMEOUT);
    }
}
