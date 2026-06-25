package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessRunnerTest {
    @TempDir
    Path tempDir;

    @Test
    void runsCommandInWorkingDirectory() throws Exception {
        Observation observation = ProcessRunner.runShell(tempDir, "pwd", Duration.ofSeconds(5));

        assertEquals(0, observation.exitCode());
        assertTrue(observation.output().contains(tempDir.toRealPath().toString()));
    }

    @Test
    void writesStdinToProcessAndCapturesOutput() throws Exception {
        Observation observation = ProcessRunner.run(
                tempDir,
                List.of("sh", "-lc", "cat > output.txt && cat output.txt"),
                Duration.ofSeconds(5),
                "hello from stdin"
        );

        assertEquals(0, observation.exitCode());
        assertTrue(observation.output().contains("hello from stdin"));
        assertEquals("hello from stdin", Files.readString(tempDir.resolve("output.txt")));
    }

    @Test
    void timesOutLongRunningCommands() throws Exception {
        Observation observation = ProcessRunner.runShell(tempDir, "sleep 2", Duration.ofMillis(50));

        assertEquals(1, observation.exitCode());
        assertTrue(observation.output().contains("timed out"));
    }

    @Test
    void applyPatchToolUsesProcessRunnerTimeoutPath() throws Exception {
        Files.writeString(tempDir.resolve("file.txt"), "before\n");
        String patch = "diff --git a/file.txt b/file.txt\n"
                + "--- a/file.txt\n"
                + "+++ b/file.txt\n"
                + "@@ -1 +1 @@\n"
                + "-before\n"
                + "+after\n";
        ToolActionHandler handler = new ToolActionHandler(ToolRegistry.defaultRegistry(), new DefaultPermissionGate());
        ToolContext context = new ToolContext(new Workspace(tempDir), "true", ToolPolicy.defaultPolicy());

        Observation observation = handler.execute(new Action.ApplyPatch(patch), context);

        assertEquals(0, observation.exitCode());
        assertEquals("after\n", Files.readString(tempDir.resolve("file.txt")));
    }
}
