package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.time.Duration;
import java.util.List;

/** Applies a model-provided unified diff through git apply. */
public final class ApplyPatchTool implements Tool {
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    @Override public String name() { return "apply_patch"; }
    @Override public RiskLevel risk() { return RiskLevel.MEDIUM; }
    @Override public boolean supports(Action.ToolAction action) { return action instanceof Action.ApplyPatch; }

    @Override
    public Observation execute(Action.ToolAction action, ToolContext context) throws Exception {
        Action.ApplyPatch applyPatch = (Action.ApplyPatch) action;
        return ProcessRunner.run(
                context.workspace().root(),
                List.of("git", "apply", "--whitespace=nowarn", "-"),
                DEFAULT_TIMEOUT,
                applyPatch.patch()
        );
    }
}
