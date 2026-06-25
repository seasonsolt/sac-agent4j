package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Runs a shell command in the workspace after permission approval. */
public final class ShellTool implements Tool {
    @Override public String name() { return "shell"; }
    @Override public RiskLevel risk() { return RiskLevel.HIGH; }
    @Override public boolean supports(Action.ToolAction action) { return action instanceof Action.Shell; }

    @Override
    public Observation execute(Action.ToolAction action, ToolContext context) throws Exception {
        Action.Shell shell = (Action.Shell) action;
        return runShell(context, shell.command());
    }

    static Observation runShell(ToolContext context, String command) throws Exception {
        Process process = new ProcessBuilder("sh", "-lc", command)
                .directory(context.workspace().root().toFile())
                .redirectErrorStream(false)
                .start();
        boolean completed = process.waitFor(Duration.ofSeconds(60).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            return Observation.failed("command timed out after 60s: " + command);
        }
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Observation(process.exitValue(), ToolSupport.truncate("stdout:\n" + stdout + "\nstderr:\n" + stderr));
    }
}
