package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolActionHandlerTest {
    @TempDir
    Path tempDir;

    @Test
    void readsAndSearchesFiles() throws Exception {
        Files.writeString(tempDir.resolve("README.md"), "hello minimal agent\n");
        ToolActionHandler handler = new ToolActionHandler(ToolRegistry.defaultRegistry(), new DefaultPermissionGate());
        ToolContext context = new ToolContext(new Workspace(tempDir), "true", ToolPolicy.defaultPolicy());

        Observation read = handler.execute(new Action.ReadFile("README.md"), context);
        assertEquals(0, read.exitCode());
        assertTrue(read.output().contains("minimal"));

        Observation search = handler.execute(new Action.Search("minimal"), context);
        assertEquals(0, search.exitCode());
        assertTrue(search.output().contains("README.md:1"));
    }

    @Test
    void searchReportsUnreadableFiles() throws Exception {
        Files.writeString(tempDir.resolve("README.md"), "hello minimal agent\n");
        Files.write(tempDir.resolve("unreadable.txt"), new byte[] {(byte) 0xFF, (byte) 0xFE, 0x00});
        ToolActionHandler handler = new ToolActionHandler(ToolRegistry.defaultRegistry(), new DefaultPermissionGate());
        ToolContext context = new ToolContext(new Workspace(tempDir), "true", ToolPolicy.defaultPolicy());

        Observation search = handler.execute(new Action.Search("minimal"), context);

        assertEquals(0, search.exitCode());
        assertTrue(search.output().contains("README.md:1"));
        assertTrue(search.output().contains("skipped 1 unreadable file"));
    }

    @Test
    void runsShellCommandInWorkspace() throws Exception {
        ToolActionHandler handler = new ToolActionHandler(ToolRegistry.defaultRegistry(), new DefaultPermissionGate());
        ToolContext context = new ToolContext(new Workspace(tempDir), "true", ToolPolicy.defaultPolicy());

        Observation observation = handler.execute(new Action.Shell("pwd"), context);

        assertEquals(0, observation.exitCode());
        assertTrue(observation.output().contains(tempDir.toRealPath().toString()));
    }

    @Test
    void rejectsShellCommandsBlockedByPolicy() throws Exception {
        ToolActionHandler handler = new ToolActionHandler(ToolRegistry.defaultRegistry(), new DefaultPermissionGate());
        ToolContext context = new ToolContext(new Workspace(tempDir), "true", ToolPolicy.defaultPolicy());

        Observation observation = handler.execute(new Action.Shell("rm -rf target"), context);

        assertEquals(1, observation.exitCode());
        assertTrue(observation.output().contains("rejected by policy"));
    }
}
