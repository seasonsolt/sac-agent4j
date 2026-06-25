package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.llm.ScriptedLlmClient;
import io.github.seasonsolt.sacagent4j.plan.TodoStatus;
import io.github.seasonsolt.sacagent4j.tool.ToolExecutor;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentLoopTest {
    @TempDir
    Path tempDir;

    @Test
    void executesScriptedTurnsUntilFinish() throws Exception {
        Files.writeString(tempDir.resolve("README.md"), "sac-agent4j\n");
        AgentLoop loop = new AgentLoop(
                new ScriptedLlmClient(List.of(
                        new Action.ReadFile("README.md"),
                        new Action.Finish("done")
                )),
                new ToolExecutor(new Workspace(tempDir), "true"),
                new ContextBuilder(new ObjectMapper()),
                4
        );

        AgentResult result = loop.run("inspect readme");
        assertTrue(result.finished());
        assertEquals("done", result.summary());
        assertEquals(1, result.history().size());
    }

    @Test
    void tracksPlanAndTodoUpdatesInsideLoop() throws Exception {
        AgentLoop loop = new AgentLoop(
                new ScriptedLlmClient(List.of(
                        new Action.SetPlan(List.of("inspect failure", "patch bug")),
                        new Action.UpdateTodo(1, TodoStatus.in_progress),
                        new Action.UpdateTodo(1, TodoStatus.completed),
                        new Action.Finish("planned")
                )),
                new ToolExecutor(new Workspace(tempDir), "true"),
                new ContextBuilder(new ObjectMapper()),
                8
        );

        AgentResult result = loop.run("plan demo");
        assertTrue(result.finished());
        assertEquals(3, result.history().size());
        assertEquals(TodoStatus.completed, loop.plan().get(0).status());
        assertEquals(TodoStatus.pending, loop.plan().get(1).status());
    }

}
