package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

/** Runs the configured verification command. */
public final class RunTestsTool implements Tool {
    @Override public String name() { return "run_tests"; }
    @Override public RiskLevel risk() { return RiskLevel.MEDIUM; }
    @Override public boolean supports(Action.ToolAction action) { return action instanceof Action.RunTests; }

    @Override
    public Observation execute(Action.ToolAction action, ToolContext context) throws Exception {
        return ProcessRunner.runShell(context.workspace().root(), context.testCommand(), ShellTool.DEFAULT_TIMEOUT);
    }
}
