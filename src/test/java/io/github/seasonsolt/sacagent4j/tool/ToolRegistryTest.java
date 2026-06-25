package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultRegistryFindsConcreteTools() {
        ToolRegistry registry = ToolRegistry.defaultRegistry();

        assertEquals("read_file", registry.find(new Action.ReadFile("README.md")).name());
        assertEquals("search", registry.find(new Action.Search("TODO")).name());
        assertEquals("shell", registry.find(new Action.Shell("pwd")).name());
        assertEquals("apply_patch", registry.find(new Action.ApplyPatch("diff")).name());
        assertEquals("run_tests", registry.find(new Action.RunTests()).name());
    }

    @Test
    void permissionGateRejectsDangerousShellBeforeExecution() throws Exception {
        ToolActionHandler handler = new ToolActionHandler(ToolRegistry.defaultRegistry(), new DefaultPermissionGate());
        ToolContext context = new ToolContext(new Workspace(tempDir), "true", ToolPolicy.defaultPolicy());

        Observation observation = handler.execute(new Action.Shell("rm -rf target"), context);

        assertEquals(1, observation.exitCode());
        assertTrue(observation.output().contains("rejected by policy"));
    }
}
