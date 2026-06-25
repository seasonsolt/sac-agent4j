package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolExecutorTest {
    @TempDir
    Path tempDir;

    @Test
    void readsAndSearchesFiles() throws Exception {
        Files.writeString(tempDir.resolve("README.md"), "hello minimal agent\n");
        ToolExecutor tools = new ToolExecutor(new Workspace(tempDir), "true");

        Observation read = tools.readFile("README.md");
        assertEquals(0, read.exitCode());
        assertTrue(read.output().contains("minimal"));

        Observation search = tools.search("minimal");
        assertEquals(0, search.exitCode());
        assertTrue(search.output().contains("README.md:1"));
    }

    @Test
    void runsShellCommandInWorkspace() throws Exception {
        ToolExecutor tools = new ToolExecutor(new Workspace(tempDir), "true");
        Observation observation = tools.shell("pwd");
        assertEquals(0, observation.exitCode());
        assertTrue(observation.output().contains(tempDir.toRealPath().toString()));
    }
}
