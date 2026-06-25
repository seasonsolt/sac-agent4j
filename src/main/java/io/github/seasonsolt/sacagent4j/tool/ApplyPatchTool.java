package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Applies a model-provided unified diff through git apply. */
public final class ApplyPatchTool implements Tool {
    @Override public String name() { return "apply_patch"; }
    @Override public RiskLevel risk() { return RiskLevel.MEDIUM; }
    @Override public boolean supports(Action.ToolAction action) { return action instanceof Action.ApplyPatch; }

    @Override
    public Observation execute(Action.ToolAction action, ToolContext context) throws Exception {
        Action.ApplyPatch applyPatch = (Action.ApplyPatch) action;
        Process process = new ProcessBuilder("git", "apply", "--whitespace=nowarn", "-")
                .directory(context.workspace().root().toFile())
                .redirectErrorStream(false)
                .start();
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(applyPatch.patch().getBytes(StandardCharsets.UTF_8));
        }
        int exit = process.waitFor();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Observation(exit, ToolSupport.truncate("stdout:\n" + stdout + "\nstderr:\n" + stderr));
    }
}
